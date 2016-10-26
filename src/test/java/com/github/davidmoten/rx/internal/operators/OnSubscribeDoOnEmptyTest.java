package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.davidmoten.rx.Actions;
import com.github.davidmoten.rx.Transformers;

import rx.Observable;
import rx.Subscription;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public final class OnSubscribeDoOnEmptyTest {

    @Test
    public void testNonEmpty() {
        Observable<String> source = Observable.just("Chicago", "Houston", "Phoenix");

        final AtomicBoolean wasCalled = new AtomicBoolean(false);

        source.compose(Transformers.doOnEmpty(new Action0() {
            @Override
            public void call() {
                wasCalled.set(true);
            }
        })).subscribe();

        assertFalse(wasCalled.get());
    }

    @Test
    public void testEmpty() {
        Observable<String> source = Observable.empty();

        final AtomicBoolean wasCalled = new AtomicBoolean(false);

        source.compose(Transformers.doOnEmpty(new Action0() {
            @Override
            public void call() {
                wasCalled.set(true);
            }
        })).subscribe();

        assertTrue(wasCalled.get());
    }

    @Test
    public void testUnsubscription() {
        final AtomicBoolean wasCalled = new AtomicBoolean(false);

        PublishSubject<Integer> source = PublishSubject.create();

        Subscription subscription = source.compose(Transformers.doOnEmpty(new Action0() {
            @Override
            public void call() {
                wasCalled.set(true);
            }
        })).take(3).subscribe();

        assertTrue(source.hasObservers());

        source.onNext(0);
        source.onNext(1);

        assertTrue(source.hasObservers());

        source.onNext(2);

        assertFalse(source.hasObservers());

        subscription.unsubscribe();

        assertFalse(wasCalled.get());
    }

    @Test
    public void testBackPressure() {

        final AtomicBoolean wasCalled = new AtomicBoolean(false);

        Observable<Integer> source = Observable.range(0, 1000)
                .compose(Transformers.<Integer> doOnEmpty(Actions.setToTrue0(wasCalled)));

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);

        source.subscribe(ts);

        ts.requestMore(1);

        assertTrue(ts.getOnNextEvents().size() == 1);
        ts.assertNotCompleted();
        assertTrue(ts.getOnErrorEvents().size() == 0);
        assertFalse(wasCalled.get());
    }

    @Test
    public void subscriberStateTest() {
        final AtomicInteger counter = new AtomicInteger(0);

        final AtomicInteger callCount = new AtomicInteger(0);

        Observable<Integer> o = Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.range(1, counter.getAndIncrement() % 2);
            }
        }).compose(Transformers.<Integer> doOnEmpty(Actions.increment0(callCount)));

        o.subscribe();
        o.subscribe();
        o.subscribe();
        o.subscribe();
        o.subscribe();

        assert (callCount.get() == 3);
    }

    @Test
    public void ifSourceEmitsErrorThenDoOnEmptyIsNotRun() {
        TestSubscriber<Object> ts = TestSubscriber.create();
        AtomicBoolean set = new AtomicBoolean(false);
        Exception ex = new Exception("boo");
        Observable.error(ex) //
                .compose(Transformers.doOnEmpty(Actions.setToTrue0(set))) //
                .subscribe(ts);
        assertFalse(set.get());
        ts.assertError(ex);
        ts.assertNoValues();
    }
    
    @Test(expected=OnErrorNotImplementedException.class)
    public void ifOnEmptyActionThrowsOnErrorNotImplementedExceptionThenSubscribeThrows() {
        Observable.empty() //
             .compose(Transformers.doOnEmpty(Actions.throw0(new OnErrorNotImplementedException(new RuntimeException())))) //
             .subscribe();
    }
    
    @Test
    public void ifOnEmptyActionThrowsNonFatalRuntimeExceptionThenErrorEmitted() {
        TestSubscriber<Object> ts = TestSubscriber.create();
        Observable.empty() //
             .compose(Transformers.doOnEmpty(Actions.throw0(new NumberFormatException()))) //
             .subscribe(ts);
        ts.assertNoValues();
        ts.assertTerminalEvent();
        ts.assertError(NumberFormatException.class);
    }
    
    @Test
    public void testUnsubscribeAfterActionButBeforeCompletionMeansStreamDoesNotComplete() {
        final TestSubscriber<Object> ts = TestSubscriber.create();
        Observable.empty() //
         .compose(Transformers.doOnEmpty(new Action0() {
            @Override
            public void call() {
                ts.unsubscribe();
            }})).subscribe(ts);
       ts.assertNoValues();
       ts.assertNotCompleted();
    }
    

}