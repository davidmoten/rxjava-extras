package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.davidmoten.rx.Actions;
import com.github.davidmoten.rx.Obs;
import com.github.davidmoten.rx.observables.CachedObservable;

import rx.Observable;

public class OnSubscribeCacheResetableTest {

    @Test
    public void test() {
        final AtomicInteger completedCount = new AtomicInteger();
        final AtomicInteger emissionCount = new AtomicInteger();
        CachedObservable<Integer> cached = Obs
                .cache(Observable.just(1).doOnCompleted(Actions.increment0(completedCount)));
        Observable<Integer> o = cached.doOnNext(Actions.increment1(emissionCount));
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
