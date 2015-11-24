package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.davidmoten.rx.exceptions.TooManySubscribersException;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action0;

public final class TransformerLimitSubscribers<T> implements Transformer<T, T> {

    private final AtomicInteger subscriberCount;
    private final int maxSubscribers;

    public TransformerLimitSubscribers(AtomicInteger subscriberCount, int maxSubscribers) {
        this.subscriberCount = subscriberCount;
        this.maxSubscribers = maxSubscribers;
    }

    @Override
    public Observable<T> call(Observable<T> o) {
        return o.doOnSubscribe(onSubscribe()).doOnUnsubscribe(onUnsubscribe());
    }

    private Action0 onSubscribe() {
        return new Action0() {

            @Override
            public void call() {
                if (subscriberCount.incrementAndGet() > maxSubscribers)
                    throw new TooManySubscribersException();
            }
        };
    }

    private Action0 onUnsubscribe() {
        return new Action0() {

            @Override
            public void call() {
                subscriberCount.decrementAndGet();
            }
        };
    }

}
