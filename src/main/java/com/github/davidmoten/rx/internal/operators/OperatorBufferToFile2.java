package com.github.davidmoten.rx.internal.operators;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import com.github.davidmoten.rx.buffertofile.CacheType;
import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.rx.buffertofile.Options;
import com.github.davidmoten.util.Preconditions;

import rx.Observable.Operator;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.internal.operators.NotificationLite;
import rx.internal.util.BackpressureDrainManager;

public class OperatorBufferToFile2<T> implements Operator<T, T> {

    private static final String QUEUE_NAME = "q";

    private final Serializer<T> serializer;
    private final Scheduler scheduler;
    private final Options options;

    public OperatorBufferToFile2(DataSerializer<T> dataSerializer, Scheduler scheduler,
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
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        File file = options.fileFactory().call();
        final DB db = createDb(file, options);
        final Queue<T> queue = getQueue(db, serializer);
        // don't pass through subscriber as we are async and doing queue
        // draining
        // a parent being unsubscribed should not affect the children
        BufferSubscriber<T> parent = new BufferSubscriber<T>(child, options, queue);

        // if child unsubscribes it should unsubscribe the parent, but not the
        // other way around
        child.add(parent);
        child.add(disposer(db));
        child.setProducer(parent.manager());

        return parent;
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
        if (options.storageSizeLimitBytes().isPresent()) {
            builder = builder.sizeLimit(options.storageSizeLimitBytes().get());
        }
        return builder.transactionDisable().deleteFilesAfterClose().make();
    }

    private static final class BufferSubscriber<T> extends Subscriber<T>
            implements BackpressureDrainManager.BackpressureQueueCallback {
        // TODO get a different queue implementation
        private final Queue<T> queue;
        private final Subscriber<? super T> child;
        private final BackpressureDrainManager manager;
        private final NotificationLite<T> on = NotificationLite.instance();

        public BufferSubscriber(Subscriber<? super T> child, Options options, Queue<T> queue) {
            this.child = child;
            this.queue = queue;
            this.manager = new BackpressureDrainManager(this);
        }

        @Override
        public void onStart() {
            request(Long.MAX_VALUE);
        }

        @Override
        public void onCompleted() {
            manager.terminateAndDrain();
        }

        @Override
        public void onError(Throwable e) {
            manager.terminateAndDrain(e);
        }

        @Override
        public void onNext(T t) {
            queue.offer(t);
            manager.drain();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean accept(Object value) {
            child.onNext((T) value);
            return false;
        }

        @Override
        public void complete(Throwable exception) {
            if (exception != null) {
                child.onError(exception);
            } else {
                child.onCompleted();
            }
        }

        @Override
        public Object peek() {
            return queue.peek();
        }

        @Override
        public Object poll() {
            return queue.poll();
        }

        protected Producer manager() {
            return manager;
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
