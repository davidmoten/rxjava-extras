package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.Transformers.ErrorAndWait;

import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

public class TransformerRetryExponentialBackoffTest {

    @Test
    public void test() {
        Exception ex = new IllegalArgumentException("boo");
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Action1<ErrorAndWait> log = new Action1<ErrorAndWait>() {

            @Override
            public void call(ErrorAndWait e) {
                System.out.println("WARN: " + e.throwable().getMessage());
                System.out.println("waiting for " + e.waitMs() + "ms");
            }
        };
        Observable.just(1, 2, 3)
                // force error after 3 emissions
                .concatWith(Observable.<Integer> error(ex))
                // retry with backoff
                .compose(Transformers.<Integer> retryExponentialBackoff(5, 10,
                        TimeUnit.MILLISECONDS, log, Schedulers.computation()))
                // go
                .subscribe(ts);

        // check results
        ts.awaitTerminalEvent();
        ts.assertError(ex);
        ts.assertValues(1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3);
    }

    @Test
    public void testWithScheduler() {
        Exception ex = new IllegalArgumentException("boo");
        TestSubscriber<Integer> ts = TestSubscriber.create();
        TestScheduler scheduler = new TestScheduler();
        Action1<ErrorAndWait> log = new Action1<ErrorAndWait>() {

            @Override
            public void call(ErrorAndWait e) {
                System.out.println("WARN: " + e.throwable().getMessage());
                System.out.println("waiting for " + e.waitMs() + "ms");
            }
        };
        Observable.just(1, 2)
                // force error after 3 emissions
                .concatWith(Observable.<Integer> error(ex))
                // retry with backoff
                .compose(Transformers.<Integer> retryExponentialBackoff(2, 1, TimeUnit.MINUTES, log,
                        scheduler))
                // go
                .subscribe(ts);
        ts.assertValues(1, 2);
        ts.assertNotCompleted();
        scheduler.advanceTimeBy(1, TimeUnit.MINUTES);
        ts.assertValues(1, 2, 1, 2);
        ts.assertNotCompleted();
        // next wait is 2 seconds so advancing by 1 should do nothing
        scheduler.advanceTimeBy(1, TimeUnit.MINUTES);
        ts.assertValues(1, 2, 1, 2);
        ts.assertNotCompleted();
        scheduler.advanceTimeBy(1, TimeUnit.MINUTES);
        ts.assertValues(1, 2, 1, 2, 1, 2);
        ts.assertError(ex);
    }

}
