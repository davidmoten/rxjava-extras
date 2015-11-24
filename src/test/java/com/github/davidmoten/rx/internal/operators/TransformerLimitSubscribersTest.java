package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.exceptions.TooManySubscribersException;

import rx.Observable;
import rx.observers.TestSubscriber;

public class TransformerLimitSubscribersTest {

    @Test
    public void testOneSubscriber() {
        List<Integer> list = Observable.just(1, 2, 3)
                .compose(Transformers.<Integer> limitSubscribers(1)).toList().toBlocking().single();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testMoreThanMaxSubscribers() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable.just(1, 2, 3).compose(Transformers.<Integer> limitSubscribers(0)).subscribe(ts);
        ts.assertNoValues();
        ts.assertError(TooManySubscribersException.class);
    }

    @Test
    public void testTwoSubscribersOk() {
        TestSubscriber<Long> ts1 = TestSubscriber.create();
        TestSubscriber<Long> ts2 = TestSubscriber.create();
        Observable<Long> o = Observable.interval(100, TimeUnit.MILLISECONDS).take(3)
                .compose(Transformers.<Long> limitSubscribers(2));
        o.subscribe(ts1);
        o.subscribe(ts2);
        ts1.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts1.assertCompleted();
        ts2.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts2.assertCompleted();
    }

    @Test
    public void testThreeSubscribersNotOk() {
        TestSubscriber<Long> ts1 = TestSubscriber.create();
        TestSubscriber<Long> ts2 = TestSubscriber.create();
        TestSubscriber<Long> ts3 = TestSubscriber.create();
        Observable<Long> o = Observable.interval(100, TimeUnit.MILLISECONDS).take(3)
                .compose(Transformers.<Long> limitSubscribers(2))
                .onErrorResumeNext(Observable.<Long> empty());
        o.subscribe(ts1);
        o.subscribe(ts2);
        o.subscribe(ts3);
        ts1.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts2.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts3.awaitTerminalEvent(3, TimeUnit.SECONDS);
        ts3.assertNoValues();
        ts3.assertCompleted();
    }

}
