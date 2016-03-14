package com.github.davidmoten.rx.internal.operators;

import java.util.Queue;

import rx.Observer;
import rx.observables.SyncOnSubscribe;

public class OnSubscribeFromQueue<T> extends SyncOnSubscribe<Queue<T>, T> {

    private final Queue<T> queue;

    public OnSubscribeFromQueue(Queue<T> queue) {
        this.queue = queue;
    }

    @Override
    protected Queue<T> generateState() {
        return this.queue;
    }

    @Override
    protected Queue<T> next(Queue<T> queue, Observer<? super T> observer) {
        T value = queue.poll();
        if (value == null) {
            observer.onCompleted();
        } else {
            observer.onNext(value);
        }
        return queue;
    }

}
