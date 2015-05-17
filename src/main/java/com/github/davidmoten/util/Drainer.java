package com.github.davidmoten.util;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Observer;
import rx.Producer;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.internal.operators.NotificationLite;

public class Drainer<T> implements Observer<T>, Producer {

    public static <T> Drainer<T> create(Queue<Object> queue, Subscription subscription,
            Worker worker, Subscriber<T> child, Producer producer) {
        return new Drainer<T>(queue, subscription, worker, child, producer);
    }

    private final Subscription subscription;
    private final Worker worker;
    private final Subscriber<T> child;
    private final Producer producer;
    private final Queue<Object> queue;

    // the status of the current stream
    private volatile boolean finished = false;

    private volatile long requested = 0;

    @SuppressWarnings("rawtypes")
    private static final AtomicLongFieldUpdater<Drainer> REQUESTED = AtomicLongFieldUpdater
            .newUpdater(Drainer.class, "requested");

    @SuppressWarnings("unused")
    private volatile long counter;

    @SuppressWarnings("rawtypes")
    private static final AtomicLongFieldUpdater<Drainer> COUNTER = AtomicLongFieldUpdater
            .newUpdater(Drainer.class, "counter");

    private volatile Throwable error;

    private final NotificationLite<T> on = NotificationLite.instance();

    private final Action0 action = new Action0() {

        @Override
        public void call() {
            pollQueue();
        }
    };

    private Drainer(Queue<Object> queue, Subscription subscription, Worker worker,
            Subscriber<T> child, Producer producer) {
        this.queue = queue;
        this.subscription = subscription;
        this.worker = worker;
        this.child = child;
        this.producer = producer;
    }

    @Override
    public void request(long n) {
        BackpressureUtils.getAndAddRequest(REQUESTED, this, n);
        drain();
    }

    @Override
    public void onNext(final T t) {
        if (subscription.isUnsubscribed()) {
            return;
        }
        if (!queue.offer(t)) {
            // this would only happen if more arrived than were requested
            onError(new MissingBackpressureException());
            return;
        }
        drain();
    }

    @Override
    public void onCompleted() {
        if (subscription.isUnsubscribed() || finished) {
            return;
        }
        finished = true;
        drain();
    }

    @Override
    public void onError(final Throwable e) {
        if (subscription.isUnsubscribed() || finished) {
            return;
        }
        error = e;
        // unsubscribe eagerly since time will pass before the scheduled onError
        // results in an unsubscribe event
        subscription.unsubscribe();
        finished = true;
        // polling thread should skip any onNext still in the queue
        drain();
    }

    private void drain() {
        if (COUNTER.getAndIncrement(this) == 0) {
            worker.schedule(action);
        }
    }

    private void pollQueue() {
        long emittedTotal = 0;
        do {
            // by setting counter = 1 here we ensure that the queue can only be
            // emptied once on every call to pollQueue. The maximum size the
            // queue can get to is the total requests to upstream that occur
            // before drainer is signalled with events
            counter = 1;
            long emitted = 0;
            long r = requested;
            while (!child.isUnsubscribed()) {
                Throwable error;
                if (finished) {
                    if ((error = this.error) != null) {
                        // errors shortcut the queue so
                        // release the elements in the queue for gc
                        queue.clear();
                        child.onError(error);
                        return;
                    } else if (queue.isEmpty()) {
                        child.onCompleted();
                        return;
                    }
                }
                if (r > 0) {
                    Object o = queue.poll();
                    if (o != null) {
                        child.onNext(on.getValue(o));
                        r--;
                        emitted++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (emitted > 0)  {
                if ( requested != Long.MAX_VALUE) {
                    REQUESTED.addAndGet(this, -emitted);
                }
                emittedTotal+=emitted;
            }
        } while (COUNTER.decrementAndGet(this) > 0);
        if (emittedTotal > 0) {
            producer.request(emittedTotal);
        }
    }

}
