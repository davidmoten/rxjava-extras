package com.github.davidmoten.rx.internal.operators;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOError;
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
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.internal.operators.BackpressureUtils;
import rx.observers.Subscribers;

public final class OperatorBufferToFile<T> implements Operator<T, T> {

    private static final String QUEUE_NAME = "q";

    private final Serializer<T> serializer;
    private final Scheduler scheduler;
    private final Options options;

    public OperatorBufferToFile(DataSerializer<T> dataSerializer, Scheduler scheduler,
            Options options) {
        Preconditions.checkNotNull(dataSerializer);
        Preconditions.checkNotNull(scheduler);
        Preconditions.checkNotNull(options);
        this.scheduler = scheduler;
        this.serializer = createSerializer(dataSerializer);
        this.options = options;
    }

    private static <T> Serializer<T> createSerializer(DataSerializer<T> dataSerializer) {
        return new MapDbSerializer<T>(dataSerializer);
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        File file = options.fileFactory().call();
        final DB db = createDb(file, options);
        final Queue<T> queue = getQueue(db, serializer);
        final AtomicReference<QueueProducer<T>> queueProducer = new AtomicReference<QueueProducer<T>>();
        final Worker worker = scheduler.createWorker();
        child.add(worker);

        Subscriber<T> sub = new BufferToFileSubscriber<T>(queueProducer);

        Observable<T> source = Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> child) {
                QueueProducer<T> qp = new QueueProducer<T>(queue, child, worker,
                        options.delayError());
                queueProducer.set(qp);
                child.setProducer(qp);
            }
        });
        child.add(sub);
        child.add(disposer(db));
        Subscriber<T> wrappedChild = Subscribers.wrap(child);
        source.unsafeSubscribe(wrappedChild);
        return sub;
    }

    private static DB createDb(File file, Options options) {
        DBMaker<?> builder = DBMaker.newFileDB(file);
        if (options.cacheType() == CacheType.NO_CACHE) {
            builder = builder.cacheDisable();
        } else if (options.cacheType() == CacheType.HARD_REF) {
            builder = builder.cacheHardRefEnable();
        } else if (options.cacheType() == CacheType.SOFT_REF) {
            builder = builder.cacheSoftRefEnable();
        } else if (options.cacheType() == CacheType.WEAK_REF) {
            builder = builder.cacheWeakRefEnable();
        } else if (options.cacheType() == CacheType.LEAST_RECENTLY_USED) {
            builder = builder.cacheLRUEnable();
        }
        if (options.cacheSizeItems().isPresent()) {
            builder = builder.cacheSize(options.cacheSizeItems().get());
        }
        if (options.storageSizeLimitMB().isPresent()) {
            // sizeLimit is expected in GB
            builder = builder.sizeLimit(options.storageSizeLimitMB().get() / 1024);
        }
        return builder.transactionDisable().deleteFilesAfterClose().make();
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

    private static class QueueProducer<T> extends AtomicLong implements Producer, Action0 {

        private static final long serialVersionUID = 2521533710633950102L;
        private final Queue<T> queue;
        private final AtomicInteger drainRequested = new AtomicInteger(0);
        private final Subscriber<? super T> child;
        private final Worker worker;
        private volatile boolean done = false;
        private volatile Throwable error = null;
        private final boolean delayError;

        QueueProducer(Queue<T> queue, Subscriber<? super T> child, Worker worker,
                boolean delayError) {
            super();
            this.queue = queue;
            this.child = child;
            this.worker = worker;
            this.delayError = delayError;
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
                worker.schedule(this);
            }
        }

        // this method executed from drain() only
        @Override
        public void call() {
            try {
                drainNow();
            } catch (IOError e) {
                if (e.getCause() != null) {
                    // unwrap IOError(IOException: no free space to expand
                    // Volume)
                    child.onError(e.getCause());
                } else {
                    child.onError(e);
                }
            } catch (Throwable t) {
                child.onError(t);
            }
        }

        private void drainNow() {
            // get the number of unsatisfied requests
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

        private boolean finished(boolean isQueueEmpty) {
            if (done) {
                Throwable t = error;
                if (isQueueEmpty) {
                    // assign volatile to a temp variable so we don't
                    // read
                    // it twice
                    if (t != null) {
                        child.onError(t);
                    } else {
                        child.onCompleted();
                    }
                    // leave drainRequested > 0 so that further drain
                    // requests are ignored
                    return true;
                } else if (t != null && !delayError) {
                    // queue is not empty but we are going to shortcut
                    // that because delayError is false
                    
                    //first clear the queu
                    queue.clear();
                    
                    //now report the error
                    child.onError(t);
                    
                    // leave drainRequested > 0 so that further drain
                    // requests are ignored
                    return true;
                } else {
                    // otherwise we need to wait for all items waiting
                    // on the queue to be requested and delivered
                    // (delayError=true)
                    return drainRequested.compareAndSet(1, 0);
                }
            } else {
                return drainRequested.compareAndSet(1, 0);
            }
        }
    }

    private static <T> BlockingQueue<T> getQueue(DB db, Serializer<T> serializer) {
        return db.createQueue(QUEUE_NAME, serializer, false);
    }

    private static Subscription disposer(final DB db) {
        return new Subscription() {

            @Override
            public void unsubscribe() {
                try {
                    // note that db is configured to attempt to delete files
                    // after close
                    db.close();
                } catch (Throwable e) {
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

    private static final class MapDbSerializer<T> implements Serializer<T>, Serializable {

        private static final long serialVersionUID = -4992031045087289671L;
        private transient final DataSerializer<T> dataSerializer;

        MapDbSerializer(DataSerializer<T> dataSerializer) {
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
