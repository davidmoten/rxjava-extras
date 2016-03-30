package com.github.davidmoten.rx.internal.operators;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

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

    public static enum CacheType {
        HARD_REF, SOFT_REF, WEAK_REF, LEAST_RECENTLY_USED, NO_CACHE;
    }

    public static final class Options {
        
        public static final int  UNLIMITED = 0;
        
        private final CacheType cacheType;
        private final int cacheSizeItems;
        private final double storageSizeLimitBytes;

        private Options(CacheType cacheType, int cacheSizeItems, double storageSizeLimitBytes){
            Preconditions.checkNotNull(cacheType);
            Preconditions.checkArgument(cacheSizeItems>=0, "cacheSizeItems cannot be negative");
            Preconditions.checkArgument(storageSizeLimitBytes>=0,"storageSizeLimitBytes cannot be negative");
            this.cacheType = cacheType;
            this.cacheSizeItems = cacheSizeItems;
            this.storageSizeLimitBytes = storageSizeLimitBytes;
        }

        public CacheType getCacheType() {
            return cacheType;
        }

        public int getCacheSizeItems() {
            return cacheSizeItems;
        }

        public double getStorageSizeLimitBytes() {
            return storageSizeLimitBytes;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private CacheType cacheType=CacheType.NO_CACHE;
            private int cacheSizeItems=UNLIMITED;
            private double storageSizeLimitBytes=UNLIMITED;

            private Builder() {
            }

            public Builder cacheType(CacheType cacheType) {
                this.cacheType = cacheType;
                return this;
            }

            public Builder cacheSizeItems(int cacheSizeItems) {
                this.cacheSizeItems = cacheSizeItems;
                return this;
            }

            public Builder storageSizeLimitBytes(double storageSizeLimitBytes) {
                this.storageSizeLimitBytes = storageSizeLimitBytes;
                return this;
            }

            public Options build() {
                return new Options(cacheType, cacheSizeItems, storageSizeLimitBytes);
            }
        }
    }

    public static interface DataSerializer<T> {
        void serialize(T t, DataOutput output) throws IOException;

        T deserialize(DataInput input, int size) throws IOException;
    }

    private static <T> Serializer<Notification<T>> createSerializer(
            DataSerializer<T> dataSerializer) {
        return new MySerializer<T>(dataSerializer);
    }

    public static final class MySerializer<T> implements Serializer<Notification<T>>, Serializable {

        private static final long serialVersionUID = -4992031045087289671L;
        private transient final DataSerializer<T> dataSerializer;

        public MySerializer(DataSerializer<T> dataSerializer) {
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

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        File file = fileFactory.call();
        DBMaker builder = DBMaker.newFileDB(file);
        if (options.cacheType==CacheType.NO_CACHE) {
            builder = builder.cacheDisable();
        } else if (options.cacheType == CacheType.HARD_REF) {
            builder = builder.cacheHardRefEnable();
        }else if (options.cacheType == CacheType.SOFT_REF) {
            builder = builder.cacheSoftRefEnable();
        }else if (options.cacheType == CacheType.WEAK_REF) {
            builder = builder.cacheWeakRefEnable();
        }else if (options.cacheType == CacheType.LEAST_RECENTLY_USED) {
            builder = builder.cacheLRUEnable();
        }
        if (options.cacheSizeItems!=Options.UNLIMITED) {
            builder = builder.cacheSize(options.cacheSizeItems);
        }
        if (options.storageSizeLimitBytes!=Options.UNLIMITED) {
            builder = builder.sizeLimit(options.storageSizeLimitBytes);
        }
        final DB db = builder.deleteFilesAfterClose().make();
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
        private final AtomicBoolean wip = new AtomicBoolean(false);
        private final Subscriber<? super T> child;
        private final Worker worker;
        private final Action0 drainAction = new Action0() {

            @Override
            public void call() {
                long r = get();
                long emitted = 0;
                while (true) {
                    while (r > 0) {
                        if (child.isUnsubscribed()) {
                            // don't touch wip to prevent more draining
                            return;
                        } else {
                            Notification<T> notification = queue.poll();
                            if (notification == null) {
                                // queue is empty
                                wip.set(false);
                                return;
                            } else {
                                // there was a notification on the queue
                                notification.accept(child);
                                if (!notification.isOnNext()) {
                                    // was terminal notification
                                    // dont' touch wip to prevent more
                                    // draining
                                    return;
                                }
                                emitted++;
                            }
                        }
                    }
                    r = addAndGet(-emitted);
                    if (r == 0L) {
                        // we're done emitting the number requested so
                        // return
                        // TODO at this point here there is a race condition
                        // where a request could mean that
                        // no drain occurs
                        wip.set(false);
                        return;
                    }
                }
            }
        };

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
            if ((n > 0 && BackpressureUtils.getAndAddRequest(this, n) == 0)
                    | wip.compareAndSet(false, true)) {
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

}
