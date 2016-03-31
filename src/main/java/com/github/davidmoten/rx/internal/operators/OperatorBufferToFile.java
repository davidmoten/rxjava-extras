package com.github.davidmoten.rx.internal.operators;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Queue;
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

    private final Serializer<T> serializer;
    private final Scheduler scheduler;
    private final Func0<File> fileFactory;
    private final Options options;

    public OperatorBufferToFile(Func0<File> fileFactory, DataSerializer<T> dataSerializer,
            Scheduler scheduler, Options options) {
        Preconditions.checkNotNull(fileFactory);
        Preconditions.checkNotNull(dataSerializer);
        Preconditions.checkNotNull(scheduler);
        Preconditions.checkNotNull(options);
        this.fileFactory = fileFactory;
        this.scheduler = scheduler;
        this.serializer = createSerializer(dataSerializer);
        this.options = options;
    }

    private static <T> Serializer<T> createSerializer(DataSerializer<T> dataSerializer) {
        return new MapDbSerializer<T>(dataSerializer);
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        File file = fileFactory.call();
        final DB db = createDb(file, options);
        final Queue<T> queue = getQueue(db, serializer);
        final AtomicReference<QueueProducer<T>> queueProducer = new AtomicReference<QueueProducer<T>>();
        final Worker worker = scheduler.createWorker();
        child.add(worker);

        Subscriber<T> sub = new BufferToFileSubscriber<T>(queueProducer);

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

    private static DB createDb(File file, Options options) {
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
        if (options.getCacheSizeItems().isPresent()) {
            builder = builder.cacheSize(options.getCacheSizeItems().get());
        }
        if (options.getStorageSizeLimitBytes().isPresent()) {
            builder = builder.sizeLimit(options.getStorageSizeLimitBytes().get());
        }
        final DB db = builder.transactionDisable().deleteFilesAfterClose().make();
        return db;
    }

    private static class BufferToFileSubscriber<T> extends Subscriber<T> {

        private final AtomicReference<QueueProducer<T>> queueProducer;

        BufferToFileSubscriber(AtomicReference<QueueProducer<T>> queueProducer) {
            this.queueProducer = queueProducer;
        }

        @Override
        public void onStart() {
            request(Long.MAX_VALUE);
        }

        @Override
        public void onCompleted() {
            queueProducer.get().onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            queueProducer.get().onError(e);
        }

        @Override
        public void onNext(T t) {
            queueProducer.get().onNext(t);
        }

    }

    private static class QueueProducer<T> extends AtomicLong implements Producer {

        private static final long serialVersionUID = 2521533710633950102L;
        private final Queue<T> queue;
        private final AtomicInteger drainRequested = new AtomicInteger(0);
        private final Subscriber<? super T> child;
        private final Worker worker;
        private volatile boolean done = false;
        private volatile Throwable error = null;
        private final Action0 drainAction = new Action0() {

            @Override
            public void call() {
                // TODO would be nice if n were requested and terminal event was
                // received after nth that terminal event was emitted as
                // well (at the moment requires another request which is still
                // compliant but not optimal)
                long r = get();
                while (true) {
                    // reset drainRequested counter
                    drainRequested.set(1);
                    long emitted = 0;
                    while (r > 0) {
                        if (child.isUnsubscribed()) {
                            // leave drainRequested > 0 to prevent more
                            // scheduling of drains
                            return;
                        } else {
                            T item = queue.poll();
                            if (item == null) {
                                // queue is empty
                                if (finished(true)) {
                                    return;
                                } else {
                                    // another drain was requested so go
                                    // round again but break out of this
                                    // while loop to the outer loop so we
                                    // can update r and reset drainRequested
                                    break;
                                }
                            } else {
                                // there was an item on the queue
                                child.onNext(item);
                                r--;
                                emitted++;
                            }
                        }
                    }
                    r = addAndGet(-emitted);
                    if (r == 0L && finished(queue.isEmpty())) {
                        return;
                    }
                }
            }
        };

        private boolean finished(boolean isQueueEmpty) {
            if (done && isQueueEmpty) {
                // assign volatile to a temp variable so we don't read it twice
                Throwable t = error;
                if (t != null) {
                    child.onError(t);
                } else {
                    child.onCompleted();
                }
                // leave drainRequested > 0 so that further drain requests are
                // ignored
                return true;
            } else {
                return drainRequested.compareAndSet(1, 0);
            }
        }

        void onNext(T t) {
            queue.offer(t);
            drain();
        }

        void onError(Throwable e) {
            // must assign error before assign done = true to avoid race
            // condition
            error = e;
            done = true;
            drain();
        }

        void onCompleted() {
            done = true;
            drain();
        }

        QueueProducer(Queue<T> queue, Subscriber<? super T> child, Worker worker) {
            super();
            this.queue = queue;
            this.child = child;
            this.worker = worker;
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                BackpressureUtils.getAndAddRequest(this, n);
                drain();
            }
        }

        private void drain() {
            // only schedule a drain if current drain has finished
            // otherwise the drainRequested counter will be incremented
            // and the drain loop will ensure that another drain cyle occurs if
            // required
            if (drainRequested.getAndIncrement() == 0) {
                worker.schedule(drainAction);
            }
        }

    }

    private static <T> BlockingQueue<T> getQueue(DB db, Serializer<T> serializer) {
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
                    // there is no facility to report unsubscription failures in
                    // the observable chain so write to stderr
                    e.printStackTrace();
                }
            }

            @Override
            public boolean isUnsubscribed() {
                return db.isClosed();
            }
        };
    }

    public static final class MapDbSerializer<T> implements Serializer<T>, Serializable {

        private static final long serialVersionUID = -4992031045087289671L;
        private transient final DataSerializer<T> dataSerializer;

        public MapDbSerializer(DataSerializer<T> dataSerializer) {
            this.dataSerializer = dataSerializer;
        }

        @Override
        public T deserialize(final DataInput input, int size) throws IOException {
            return dataSerializer.deserialize(input, size);
        }

        @Override
        public int fixedSize() {
            return -1;
        }

        @Override
        public void serialize(DataOutput output, T t) throws IOException {
            dataSerializer.serialize(output, t);
        }
    };


}
