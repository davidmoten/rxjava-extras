package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.Transformer;
import rx.Scheduler.Worker;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public final class TransformerLimitSubscribers<T> implements Transformer<T, T> {

    private final AtomicInteger subscriberCount;
    private final int maxSubscribers;
    private final long delayMs;

    public TransformerLimitSubscribers(AtomicInteger subscriberCount, int maxSubscribers,
            long delayMs) {
        this.subscriberCount = subscriberCount;
        this.maxSubscribers = maxSubscribers;
        this.delayMs = delayMs;
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
                Worker worker = Schedulers.computation().createWorker();
                worker.schedule(new Action0() {

                    @Override
                    public void call() {
                        subscriberCount.decrementAndGet();
                    }
                });
            }
        };
    }

    public static class TooManySubscribersException extends RuntimeException {
    }

}
