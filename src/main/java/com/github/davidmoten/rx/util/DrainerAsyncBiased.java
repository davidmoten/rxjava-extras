package com.github.davidmoten.rx.util;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

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
 *            type of the items being queued and emitted by this drainer
 */
public class DrainerAsyncBiased<T> implements Drainer<T> {

    public static <T> DrainerAsyncBiased<T> create(Queue<Object> queue, Subscription subscription,
            Worker worker, Subscriber<? super T> child) {
        return new DrainerAsyncBiased<T>(queue, subscription, worker, child);
    }

    private final Subscription subscription;
    private final Worker worker;
    private final Subscriber<? super T> child;
    private final Queue<Object> queue;

    // the status of the current stream
    private volatile boolean finished = false;

    private final AtomicReference<ExpectedAndSurplus> counts = new AtomicReference<ExpectedAndSurplus>(
            new ExpectedAndSurplus(0, 0));

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
            Subscriber<? super T> child) {
        this.queue = queue;
        this.subscription = subscription;
        this.worker = worker;
        this.child = child;
    }

    @Override
    public void request(long n) {
        if (n <= 0)
            return;
        addToCounts(n, -n);
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
        addToCounts(0, 1);
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
        long r = counts.get().expected;
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
                    r = addToCounts(-emitted, emitted).expected;
                }
            } else if (COUNTER.decrementAndGet(this) == 0) {
                break;
            } else {
                // update r for the next time through the loop
                r = counts.get().expected;
            }
        }
    }

    @Override
    public long surplus() {
        return 0;
    }

    private static final class ExpectedAndSurplus {
        final long expected;
        final long surplus;

        ExpectedAndSurplus(long expected, long surplus) {
            this.expected = expected;
            this.surplus = surplus;
        }
    }

    private ExpectedAndSurplus addToCounts(long expectedToAdd, long surplusToAdd) {
        while (true) {
            ExpectedAndSurplus c = counts.get();
            long ex = c.expected + expectedToAdd;
            if (ex < 0)
                ex = Long.MAX_VALUE;
            ExpectedAndSurplus c2 = new ExpectedAndSurplus(ex, c.surplus + surplusToAdd);
            if (counts.compareAndSet(c, c2))
                return c2;
        }
    }

}
