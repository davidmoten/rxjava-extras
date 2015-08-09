package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.davidmoten.rx.RetryWhen;
import com.github.davidmoten.rx.RetryWhen.ErrorAndDuration;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
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

    @SuppressWarnings("unchecked")
    @Test
    public void testRetryWhenSpecificExceptionFails() {
        Exception ex = new IllegalArgumentException("boo");
        TestSubscriber<Integer> ts = TestSubscriber.create();
        TestScheduler scheduler = new TestScheduler();
        Observable.just(1, 2)
                // force error after 3 emissions
                .concatWith(Observable.<Integer> error(ex))
                // retry with backoff
                .retryWhen(RetryWhen.maxRetries(2).action(log)
                        .exponentialBackoff(1, TimeUnit.MINUTES).scheduler(scheduler)
                        .failWhenInstanceOf(IllegalArgumentException.class).build())
                // go
                .subscribe(ts);
        ts.assertValues(1, 2);
        ts.assertError(ex);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetryWhenSpecificExceptionFailsBecauseIsNotInstanceOf() {
        Exception ex = new IllegalArgumentException("boo");
        TestSubscriber<Integer> ts = TestSubscriber.create();
        TestScheduler scheduler = new TestScheduler();
        Observable.just(1, 2)
                // force error after 3 emissions
                .concatWith(Observable.<Integer> error(ex))
                // retry with backoff
                .retryWhen(RetryWhen.maxRetries(2).action(log)
                        .exponentialBackoff(1, TimeUnit.MINUTES).scheduler(scheduler)
                        .retryWhenInstanceOf(SQLException.class).build())
                // go
                .subscribe(ts);
        ts.assertValues(1, 2);
        ts.assertError(ex);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRetryWhenSpecificExceptionAllowed() {
        Exception ex = new IllegalArgumentException("boo");
        TestSubscriber<Integer> ts = TestSubscriber.create();
        TestScheduler scheduler = new TestScheduler();
        Observable.just(1, 2)
                // force error after 3 emissions
                .concatWith(Observable.<Integer> error(ex))
                // retry with backoff
                .retryWhen(RetryWhen.maxRetries(2).action(log)
                        .exponentialBackoff(1, TimeUnit.MINUTES).scheduler(scheduler)
                        .retryWhenInstanceOf(IllegalArgumentException.class).build())
                // go
                .subscribe(ts);
        ts.assertValues(1, 2);
        ts.assertNotCompleted();
    }

    private static final Action1<ErrorAndDuration> log = new Action1<ErrorAndDuration>() {

        @Override
        public void call(ErrorAndDuration e) {
            System.out.println("WARN: " + e.throwable().getMessage());
            System.out.println("waiting for " + e.durationMs() + "ms");
        }
    };

    @Test
    public void testRetryWhenSpecificExceptionAllowedUsePredicateReturnsTrue() {
        Exception ex = new IllegalArgumentException("boo");
        TestSubscriber<Integer> ts = TestSubscriber.create();
        TestScheduler scheduler = new TestScheduler();
        Func1<Throwable, Boolean> predicate = new Func1<Throwable, Boolean>() {
            @Override
            public Boolean call(Throwable t) {
                return t instanceof IllegalArgumentException;
            }
        };
        Observable.just(1, 2)
                // force error after 3 emissions
                .concatWith(Observable.<Integer> error(ex))
                // retry with backoff
                .retryWhen(
                        RetryWhen.maxRetries(2).action(log).exponentialBackoff(1, TimeUnit.MINUTES)
                                .scheduler(scheduler).retryIf(predicate).build())
                // go
                .subscribe(ts);
        ts.assertValues(1, 2);
        ts.assertNotCompleted();
    }

    @Test
    public void testRetryWhenSpecificExceptionAllowedUsePredicateReturnsFalse() {
        Exception ex = new IllegalArgumentException("boo");
        TestSubscriber<Integer> ts = TestSubscriber.create();
        TestScheduler scheduler = new TestScheduler();
        Func1<Throwable, Boolean> predicate = new Func1<Throwable, Boolean>() {
            @Override
            public Boolean call(Throwable t) {
                return false;
            }
        };
        Observable.just(1, 2)
                // force error after 3 emissions
                .concatWith(Observable.<Integer> error(ex))
                // retry with backoff
                .retryWhen(
                        RetryWhen.maxRetries(2).action(log).exponentialBackoff(1, TimeUnit.MINUTES)
                                .scheduler(scheduler).retryIf(predicate).build())
                // go
                .subscribe(ts);
        ts.assertValues(1, 2);
        ts.assertError(ex);
    }
}
