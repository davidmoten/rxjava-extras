package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.davidmoten.rx.RetryWhen;
import com.github.davidmoten.util.ErrorAndDuration;

import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

public class RetryWhenTest {

    @Test
    public void testExponentialBackoff() {
        Exception ex = new IllegalArgumentException("boo");
        TestSubscriber<Integer> ts = TestSubscriber.create();
        final AtomicInteger logCalls = new AtomicInteger();
        Action1<ErrorAndDuration> log = new Action1<ErrorAndDuration>() {

            @Override
            public void call(ErrorAndDuration e) {
                System.out.println("WARN: " + e.throwable().getMessage());
                System.out.println("waiting for " + e.durationMs() + "ms");
                logCalls.incrementAndGet();
            }
        };
        Observable.just(1, 2, 3)
                // force error after 3 emissions
                .concatWith(Observable.<Integer> error(ex))
                // retry with backoff
                .retryWhen(RetryWhen.maxRetries(5).action(log)
                        .exponentialBackoff(10, TimeUnit.MILLISECONDS).build())
                // go
                .subscribe(ts);

        // check results
        ts.awaitTerminalEvent();
        ts.assertError(ex);
        ts.assertValues(1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3);
        assertEquals(5, logCalls.get());
    }

    @Test
    public void testWithScheduler() {
        Exception ex = new IllegalArgumentException("boo");
        TestSubscriber<Integer> ts = TestSubscriber.create();
        TestScheduler scheduler = new TestScheduler();
        Action1<ErrorAndDuration> log = new Action1<ErrorAndDuration>() {

            @Override
            public void call(ErrorAndDuration e) {
                System.out.println("WARN: " + e.throwable().getMessage());
                System.out.println("waiting for " + e.durationMs() + "ms");
            }
        };
        Observable.just(1, 2)
                // force error after 3 emissions
                .concatWith(Observable.<Integer> error(ex))
                // retry with backoff
                .retryWhen(RetryWhen.maxRetries(2).action(log)
                        .exponentialBackoff(1, TimeUnit.MINUTES).scheduler(scheduler).build())
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
