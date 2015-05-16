package com.github.davidmoten.util;

import java.util.Queue;

import rx.Observer;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.internal.operators.NotificationLite;

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
            if (busy)
                return;
            else
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
            if (busy)
                return;
            else
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
            if (busy)
                return;
            else
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

    private void drain() {
        long emittedTotal = 0;
        do {
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
                        child.onNext((T) on.getValue(o));
                        r--;
                        emittedTotal++;
                        emitted++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (emitted > 0 && requested != Long.MAX_VALUE) {
                requested -= emitted;
            }
        } while (--counter > 0);
        if (emittedTotal > 0) {
            producer.request(emittedTotal);
        }

    }

}
