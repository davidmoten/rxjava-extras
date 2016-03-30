package com.github.davidmoten.rx.internal.operators;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import com.github.davidmoten.rx.buffertofile.CacheType;
import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.rx.buffertofile.Options;
import com.github.davidmoten.util.Preconditions;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Producer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.internal.operators.BackpressureUtils;
import rx.observers.Subscribers;

public class OperatorBufferToFile<T> implements Operator<T, T> {

    private static final String QUEUE_NAME = "q";

    private final Serializer<Notification<T>> serializer;
    private final Scheduler scheduler;
    private final Func0<File> fileFactory;
    private final Options options;

    public OperatorBufferToFile(Func0<File> fileFactory, DataSerializer<T> dataSerializer,
            Scheduler scheduler, Options options) {
        this.fileFactory = fileFactory;
        Preconditions.checkNotNull(fileFactory);
        Preconditions.checkNotNull(dataSerializer);
        Preconditions.checkNotNull(scheduler);
        this.scheduler = scheduler;
        this.serializer = createSerializer(dataSerializer);
        this.options = options;
    }

    private static <T> Serializer<Notification<T>> createSerializer(
            DataSerializer<T> dataSerializer) {
        return new MapDbSerializer<T>(dataSerializer);
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        File file = fileFactory.call();
        final DB db = createDb(file);
        final BlockingQueue<Notification<T>> queue = getQueue(db, serializer);
        final AtomicReference<QueueProducer<T>> queueProducer = new AtomicReference<QueueProducer<T>>();
        final Worker worker = scheduler.createWorker();
        child.add(worker);

        Subscriber<T> sub = new BufferToFileSubscriber<T>(queue, queueProducer);

        Observable<T> source = Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> child) {
                QueueProducer<T> qp = new QueueProducer<T>(queue, child, worker);
                queueProducer.set(qp);
                child.setProducer(qp);
            }
        });
        child.add(sub);
        child.add(disposer(db, file));
        Subscriber<T> wrappedChild = Subscribers.wrap(child);
        source.subscribe(wrappedChild);
        return sub;
    }

    private DB createDb(File file) {
        DBMaker<?> builder = DBMaker.newFileDB(file);
        if (options.getCacheType() == CacheType.NO_CACHE) {
            builder = builder.cacheDisable();
        } else if (options.getCacheType() == CacheType.HARD_REF) {
            builder = builder.cacheHardRefEnable();
        } else if (options.getCacheType() == CacheType.SOFT_REF) {
            builder = builder.cacheSoftRefEnable();
        } else if (options.getCacheType() == CacheType.WEAK_REF) {
            builder = builder.cacheWeakRefEnable();
        } else if (options.getCacheType() == CacheType.LEAST_RECENTLY_USED) {
            builder = builder.cacheLRUEnable();
        }
        if (options.getCacheSizeItems() != Options.UNLIMITED) {
            builder = builder.cacheSize(options.getCacheSizeItems());
        }
        if (options.getStorageSizeLimitBytes() != Options.UNLIMITED) {
            builder = builder.sizeLimit(options.getStorageSizeLimitBytes());
        }
        final DB db = builder.deleteFilesAfterClose().transactionDisable().make();
        return db;
    }

    private static class BufferToFileSubscriber<T> extends Subscriber<T> {

        private final BlockingQueue<Notification<T>> queue;
        private final AtomicReference<QueueProducer<T>> queueProducer;

        public BufferToFileSubscriber(BlockingQueue<Notification<T>> queue,
                AtomicReference<QueueProducer<T>> queueProducer) {
            this.queue = queue;
            this.queueProducer = queueProducer;
        }

        @Override
        public void onStart() {
            request(Long.MAX_VALUE);
        }

        @Override
        public void onCompleted() {
            queue.offer(Notification.<T> createOnCompleted());
            messageArrived();
        }

        @Override
        public void onError(Throwable e) {
            // TODO optionally shortcut error (so queue elements don't get
            // processed)
            queue.offer(Notification.<T> createOnError(e));
            messageArrived();
        }

        @Override
        public void onNext(T t) {
            queue.offer(Notification.createOnNext(t));
            messageArrived();
        }

        private void messageArrived() {
            queueProducer.get().drain();
        }

    }

    private static class QueueProducer<T> extends AtomicLong implements Producer {

        private static final long serialVersionUID = 2521533710633950102L;
        private final BlockingQueue<Notification<T>> queue;
        private final AtomicInteger drainRequested = new AtomicInteger(0);
        private final Subscriber<? super T> child;
        private final Worker worker;
        private final Action0 drainAction = new Action0() {

            @Override
            public void call() {
                // TODO would be nice if 3 were requested and terminal event was
                // received after third that terminal event was emitted as
                // well
                while (true) {
                    drainRequested.set(1);
                    long r = get();
                    while (true) {
                        long emitted = 0;
                        while (r > 0) {
                            if (child.isUnsubscribed()) {
                                // leave drainRequested > 0 to prevent more
                                // scheduling of drains
                                return;
                            } else {
                                Notification<T> notification = queue.poll();
                                if (notification == null) {
                                    // queue is empty
                                    if (finished()) {
                                        return;
                                    } else {
                                        break;
                                    }
                                } else {
                                    // there was a notification on the queue
                                    notification.accept(child);
                                    if (!notification.isOnNext()) {
                                        // was terminal notification
                                        // dont' touch wip to prevent more
                                        // draining
                                        return;
                                    }
                                    r--;
                                    emitted++;
                                }
                            }
                        }
                        r = addAndGet(-emitted);
                        if (r == 0L) {
                            // we're done emitting the number requested so
                            // return but we need to check that another request
                            // hasn't just occurred to be sure it gets drained
                            if (finished()) {
                                return;
                            }
                        }
                    }
                }
            }
        };

        private boolean finished() {
            while (true) {
                if (drainRequested.get() > 1) {
                    // another drain was requested
                    return false;
                } else if (drainRequested.compareAndSet(1, 0)) {
                    return true;
                }
            }
        }

        QueueProducer(BlockingQueue<Notification<T>> queue, Subscriber<? super T> child,
                Worker worker) {
            super();
            this.queue = queue;
            this.child = child;
            this.worker = worker;
        }

        @Override
        public void request(long n) {
            drain(n);
        }

        void drain() {
            drain(0);
        }

        private void drain(long n) {
            // only one thread at a time
            // the or wip.compareAndSet handles the case where the drainAction
            // hasn't finished but it has just set the wip to false
            if (n > 0) {
                BackpressureUtils.getAndAddRequest(this, n);
            }
            if (drainRequested.getAndAdd(1) == 0) {
                worker.schedule(drainAction);
            }
        }

    }

    private static <T> BlockingQueue<Notification<T>> getQueue(DB db,
            Serializer<Notification<T>> serializer) {
        return db.createQueue(QUEUE_NAME, serializer, false);
    }

    private static Subscription disposer(final DB db, final File file) {
        return new Subscription() {

            @Override
            public void unsubscribe() {
                try {
                    // note that db is configured to attempt to delete files
                    // after close
                    db.close();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public boolean isUnsubscribed() {
                return db.isClosed();
            }
        };
    }

    public static final class MapDbSerializer<T>
            implements Serializer<Notification<T>>, Serializable {

        private static final long serialVersionUID = -4992031045087289671L;
        private transient final DataSerializer<T> dataSerializer;

        public MapDbSerializer(DataSerializer<T> dataSerializer) {
            this.dataSerializer = dataSerializer;
        }

        @Override
        public Notification<T> deserialize(DataInput input, int size) throws IOException {
            byte type = input.readByte();
            if (type == 0) {
                return Notification.createOnCompleted();
            } else if (type == 1) {
                String errorClass = input.readUTF();
                String message = input.readUTF();
                // TODO exceptions are serializable so we should be able to
                // handle this
                return Notification.createOnError(new RuntimeException(errorClass + ":" + message));
            } else {
                // reduce size by 1 because we have read one byte already
                T t = dataSerializer.deserialize(input, size - 1);
                return Notification.createOnNext(t);
            }
        }

        @Override
        public int fixedSize() {
            return -1;
        }

        @Override
        public void serialize(DataOutput output, Notification<T> n) throws IOException {
            if (n.isOnCompleted()) {
                output.writeByte(0);
            } else if (n.isOnError()) {
                output.writeByte(1);
                output.writeUTF(n.getThrowable().getClass().getName());
                output.writeUTF(n.getThrowable().getMessage());
            } else {
                output.writeByte(2);
                dataSerializer.serialize(n.getValue(), output);
            }
        }
    };

}
