package com.github.davidmoten.util;

import java.util.Queue;

import rx.Observer;
import rx.Producer;
import rx.exceptions.MissingBackpressureException;

public class Drainer2<T> implements Observer<T>, Producer {

    private final Queue<T> queue;

    public Drainer2(Queue<T> queue) {
        this.queue = queue;
    }

    private long requested;
    private boolean busy;
    private boolean finished;
    private Throwable error;

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
        while (true) {

        }
    }

}
