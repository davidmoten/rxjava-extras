package com.github.davidmoten.util;

import java.util.Queue;

import rx.Observer;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.internal.operators.NotificationLite;

/**
 * Optimized for when request method is called on same thread as the Observer
 * methods.
 * 
 * @param <T>
 */
public class Drainer2<T> implements Observer<T>, Producer {

    private final NotificationLite<Object> on = NotificationLite.instance();
    private final Queue<T> queue;
    private final Subscriber<T> child;
    private final Producer producer;

    public Drainer2(Queue<T> queue, Subscriber<T> child, Producer producer) {
        this.queue = queue;
        this.child = child;
        this.producer = producer;
    }

    private long requested;
    private boolean busy;
    private boolean finished;
    private Throwable error;
    private long counter;

    @Override
    public void request(long n) {
        if (n <= 0)
            return;
        synchronized (this) {
            requested += n;
            if (requested < 0) {
                requested = Long.MAX_VALUE;
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
            onError(new MissingBackpressureException());
        } else {
            drain();
        }
    }

    @SuppressWarnings("unchecked")
    private void drain() {
        long emittedTotal = 0;
        long r;
        synchronized (this) {
            r = requested;
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
                emittedTotal += emitted;
                // interested in initial request being Long.MAX_VALUE rather
                // than accumulated requests reaching Long.MAX_VALUE so is fine
                // just to test the value of `r` instead of `requested`.
                if (r != Long.MAX_VALUE) {
                    synchronized (this) {
                        requested -= emitted;
                        r = requested;
                    }
                }
            } else {
                synchronized (this) {
                    if (--counter == 0) {
                        break;
                    } else {
                        // update r for the next time through the loop
                        r = requested;
                    }
                }
            }
        }
        if (emittedTotal > 0) {
            producer.request(emittedTotal);
        }
    }

}
