package com.github.davidmoten.rx.internal.operators;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.internal.operators.BackpressureUtils;
import rx.observers.Subscribers;

public class OperatorBufferToFile<T> implements Operator<T, T> {

    private static final String QUEUE_NAME = "q";

    private final File file;
    private final Serializer<Notification<T>> serializer;

    public OperatorBufferToFile(File file, Serializer<Notification<T>> serializer) {
        this.file = file;
        this.serializer = serializer;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final DB db = DBMaker.newFileDB(file).make();
        final BlockingQueue<Notification<T>> queue = getQueue(db, serializer);
        final AtomicReference<QueueProducer<T>> queueProducer = new AtomicReference<QueueProducer<T>>();
        Subscriber<T> sub = new Subscriber<T>() {

            // requests Long.MAX_VALUE by default

            @Override
            public void onCompleted() {
                queue.offer(Notification.<T> createOnCompleted());
                messageArrived();
            }

            @Override
            public void onError(Throwable e) {
                //TODO optionally shortcut error (so queue elements don't get processed)
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

        };

        Observable<T> source = Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> child) {
                QueueProducer<T> qp = new QueueProducer<T>(queue, child);
                queueProducer.set(qp);
                child.setProducer(qp);
            }
        });

        Subscriber<T> wrappedChild = Subscribers.wrap(child);
        child.add(sub);
        child.add(disposer(db));
        source.subscribe(wrappedChild);
        return sub;
    }

    private static class QueueProducer<T> extends AtomicLong implements Producer {

        private static final long serialVersionUID = 2521533710633950102L;
        private final BlockingQueue<Notification<T>> queue;
        private final AtomicBoolean wip = new AtomicBoolean(false);
        private final Subscriber<? super T> child;

        QueueProducer(BlockingQueue<Notification<T>> queue, Subscriber<? super T> child) {
            super();
            this.queue = queue;
            this.child = child;
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                BackpressureUtils.getAndAddRequest(this, n);
                drain();
            }
        }

        public void drain() {
            // only one thread at a time
            if (wip.compareAndSet(false, true)) {
                try {
                    long r = get();
                    while (true) {
                        while (r > 0) {
                            if (child.isUnsubscribed()) {
                                return;
                            } else {
                                Notification<T> notification = queue.poll();
                                if (notification == null) {
                                    // queue is empty
                                    return;
                                } else {
                                    // there was a notification on the queue
                                    notification.accept(child);
                                    if (!notification.isOnNext()) {
                                        // was terminal notification
                                        return;
                                    }
                                    r--;
                                }
                            }
                        }
                        r = addAndGet(-r);
                        if (r == 0L) {
                            // we're done emitting the number requested so
                            // return
                            return;
                        }
                    }
                } finally {
                    wip.set(false);
                }
            }
        }

    }

    private static <T> BlockingQueue<Notification<T>> getQueue(DB db,
            Serializer<Notification<T>> serializer) {
        synchronized (db) {
            if (db.getCatalog().containsKey(QUEUE_NAME)) {
                return db.getQueue(QUEUE_NAME);
            } else {
                return db.createQueue(QUEUE_NAME, serializer, false);
            }
        }
    }

    private static Subscription disposer(final DB db) {
        return new Subscription() {

            @Override
            public void unsubscribe() {
                db.close();
            }

            @Override
            public boolean isUnsubscribed() {
                return db.isClosed();
            }
        };
    }

}
