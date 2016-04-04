package com.github.davidmoten.rx.perf;

import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.infra.Blackhole;

import rx.Observer;

public class LatchedObserver<T> implements Observer<T> {

    public CountDownLatch latch = new CountDownLatch(1);
    private final Blackhole bh;

    public LatchedObserver(Blackhole bh) {
        this.bh = bh;
    }

    @Override
    public void onCompleted() {
        latch.countDown();
    }

    @Override
    public void onError(Throwable e) {
        latch.countDown();
    }

    @Override
    public void onNext(T t) {
        bh.consume(t);
    }

}
