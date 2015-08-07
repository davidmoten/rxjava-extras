package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;

import com.github.davidmoten.rx.Obs;
import com.github.davidmoten.rx.observables.CachedObservable;

public class OnSubscribeCacheResetableTest {

    @Test
    public void test() {
        final AtomicInteger completedCount = new AtomicInteger();
        final AtomicInteger emissionCount = new AtomicInteger();
        CachedObservable<Integer> cached = Obs.cache(Observable.just(1).doOnCompleted(
                new Action0() {
                    @Override
                    public void call() {
                        completedCount.incrementAndGet();
                    }
                }));
        Observable<Integer> o = cached.doOnNext(new Action1<Integer>() {

            @Override
            public void call(Integer n) {
                emissionCount.incrementAndGet();
            }
        });
        o.subscribe();
        assertEquals(1, completedCount.get());
        assertEquals(1, emissionCount.get());
        o.subscribe();
        assertEquals(1, completedCount.get());
        assertEquals(2, emissionCount.get());
        cached.reset();
        o.subscribe();
        assertEquals(2, completedCount.get());
        assertEquals(3, emissionCount.get());
    }

}
