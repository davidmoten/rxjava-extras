package com.github.davidmoten.rx.util;

import java.util.Queue;

import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.internal.operators.NotificationLite;

/**
 * Optimized for when request method is called on same thread as the Observer
 * methods.
 * 
 * @param <T>
 *            type of the items being queued and emitted by this drainer
 */
public final class DrainerSyncBiased<T> implements Drainer<T> {

    private final NotificationLite<Object> on = NotificationLite.instance();
    private final Queue<T> queue;
    private final Subscriber<? super T> child;

    public static <T> DrainerSyncBiased<T> create(Queue<T> queue, Subscriber<? super T> child) {
        return new DrainerSyncBiased<T>(queue, child);
    }

    private DrainerSyncBiased(Queue<T> queue, Subscriber<? super T> child) {
        this.queue = queue;
        this.child = child;
    }

    private long expected;
    private boolean busy;
    private boolean finished;
    private Throwable error;
    private long counter;
    // this is the value we take off any new request so that only what is
    // required is requested of upstream
    private long surplus;// expected + numOnNext - totalRequested

    @Override
    public void request(long n) {
        if (n <= 0)
            return;
        synchronized (this) {
            long expectedBefore = expected;
            expected += n;
            if (expected < 0) {
                expected = Long.MAX_VALUE;
            }
            long diff = expected - expectedBefore;
            if (surplus > 0) {
                if (diff > n) {
                    surplus += diff - n;
                    if (surplus < 0)
                        surplus = Long.MAX_VALUE;
                }
            } else {
                surplus += diff - n;
                if (diff < n) {
                    if (surplus > 0)
                        surplus = Long.MIN_VALUE;
                }
            }
            if (busy) {
                counter++;
                return;
            } else
                busy = true;
        }
        try {
            drain();
        } finally {
            synchronized (this) {
                busy = false;
            }
        }

    }

    @Override
    public void onCompleted() {
        synchronized (this) {
            finished = true;
            if (busy) {
                counter++;
                return;
            } else
                busy = true;
        }
        try {
            drain();
        } finally {
            synchronized (this) {
                busy = false;
            }
        }
    }

    @Override
    public void onError(Throwable e) {
        synchronized (this) {
            error = e;
            finished = true;
            if (busy) {
                counter++;
                return;
            } else
                busy = true;
        }
        try {
            drain();
        } finally {
            synchronized (this) {
                busy = false;
            }
        }
    }

    @Override
    public void onNext(T t) {
        if (!queue.offer(t)) {
            // this should only happen when the queue is bounded
            onError(new MissingBackpressureException());
        } else {
            synchronized (this) {
                surplus++;
            }
            drain();
        }
    }

    @SuppressWarnings("unchecked")
    private void drain() {
        long r;
        synchronized (this) {
            r = expected;
        }
        while (true) {
            long emitted = 0;
            synchronized (this) {
                counter = 1;
            }
            while (!child.isUnsubscribed()) {
                boolean isFinished;
                synchronized (this) {
                    isFinished = finished;
                }
                if (isFinished) {
                    Throwable error;
                    synchronized (this) {
                        error = this.error;
                    }
                    if (error != null) {
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
                        child.onNext((T) on.getValue(o));
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
                    synchronized (this) {
                        expected -= emitted;
                        r = expected;
                    }
                }
            } else {
                synchronized (this) {
                    if (--counter == 0) {
                        break;
                    } else {
                        // update r for the next time through the loop
                        r = expected;
                    }
                }
            }
        }
    }

    @Override
    public long surplus() {
        synchronized (this) {
            return surplus;
        }
    }

}
