package com.github.davidmoten.util;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Producer;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action0;
import rx.internal.operators.NotificationLite;

/**
 * Optimized for when request method is called on a different thread from the
 * Observer methods.
 * 
 * @param <T>
 */
public class DrainerAsyncBiased<T> implements Drainer<T> {

    public static <T> DrainerAsyncBiased<T> create(Queue<Object> queue, Subscription subscription,
            Worker worker, Subscriber<? super T> child, Producer producer) {
        return new DrainerAsyncBiased<T>(queue, subscription, worker, child, producer);
    }

    private final Subscription subscription;
    private final Worker worker;
    private final Subscriber<? super T> child;
    private final Producer producer;
    private final Queue<Object> queue;

    // the status of the current stream
    private volatile boolean finished = false;

    private volatile long requested = 0;

    @SuppressWarnings("rawtypes")
    private static final AtomicLongFieldUpdater<DrainerAsyncBiased> REQUESTED = AtomicLongFieldUpdater
            .newUpdater(DrainerAsyncBiased.class, "requested");

    @SuppressWarnings("unused")
    private volatile long counter;

    @SuppressWarnings("rawtypes")
    private static final AtomicLongFieldUpdater<DrainerAsyncBiased> COUNTER = AtomicLongFieldUpdater
            .newUpdater(DrainerAsyncBiased.class, "counter");

    private volatile Throwable error;

    private final NotificationLite<T> on = NotificationLite.instance();

    private final Action0 action = new Action0() {

        @Override
        public void call() {
            pollQueue();
        }
    };

    private DrainerAsyncBiased(Queue<Object> queue, Subscription subscription, Worker worker,
            Subscriber<? super T> child, Producer producer) {
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
        int emittedTotal = 0;
        long r = requested;
        while (true) {
            counter = 1;
            long emitted = 0;
            for (;;) {
                if (child.isUnsubscribed())
                    return;
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
            if (emitted > 0) {
                // interested in initial request being Long.MAX_VALUE rather
                // than accumulated requests reaching Long.MAX_VALUE so is fine
                // just to test the value of `r` instead of `requested`.
                if (r != Long.MAX_VALUE) {
                    r = REQUESTED.addAndGet(this, -emitted);
                }
                emittedTotal += emitted;
            } else if (COUNTER.decrementAndGet(this) == 0) {
                break;
            } else {
                // update r for the next time through the loop
                r = REQUESTED.get(this);
            }
        }
        if (emittedTotal > 0) {
            producer.request(emittedTotal);
        }
    }

}
