package com.github.davidmoten.rx.internal.operators;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OrderedMergeTest {

    @Test
    public void testSymmetricMerge() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
        Observable<Integer> o2 = Observable.just(2, 4, 6, 8);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o1, o2)).subscribe(ts);

        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    public void testAsymmetricMerge() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
        Observable<Integer> o2 = Observable.just(2, 4);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o1, o2)).subscribe(ts);

        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValues(1, 2, 3, 4, 5, 7);
    }

    @Test
    public void testSymmetricMergeAsync() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7).observeOn(Schedulers.computation());
        Observable<Integer> o2 = Observable.just(2, 4, 6, 8).observeOn(Schedulers.computation());

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o1, o2)).subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    public void testAsymmetricMergeAsync() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7).observeOn(Schedulers.computation());
        Observable<Integer> o2 = Observable.just(2, 4).observeOn(Schedulers.computation());

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o1, o2)).subscribe(ts);

        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValues(1, 2, 3, 4, 5, 7);
    }

    @Test
    public void testEmptyEmpty() {
        Observable<Integer> o1 = Observable.empty();
        Observable<Integer> o2 = Observable.empty();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o1, o2)).subscribe(ts);

        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertNoValues();
    }

    @Test
    public void testEmptySomething1() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
        Observable<Integer> o2 = Observable.empty();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o1, o2)).subscribe(ts);

        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValues(1, 3, 5, 7);
    }

    @Test
    public void testEmptySomething2() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
        Observable<Integer> o2 = Observable.empty();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o2, o1)).subscribe(ts);

        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValues(1, 3, 5, 7);
    }

    private static class TestException extends Exception {

    }

    @Test
    public void testErrorInMiddle() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
        Observable<Integer> o2 = Observable.just(2)
                .concatWith(Observable.<Integer> error(new TestException()));

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o1, o2)).subscribe(ts);

        ts.assertError(TestException.class);
        ts.assertNotCompleted();
        ts.assertValues(1, 2);
    }

    @Test
    public void testErrorImmediately() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
        Observable<Integer> o2 = Observable.<Integer> error(new TestException());

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o1, o2)).subscribe(ts);

        ts.assertError(TestException.class);
        ts.assertNotCompleted();
        ts.assertNoValues();
    }

    @Test
    public void testErrorInMiddleDelayed() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
        Observable<Integer> o2 = Observable.just(2)
                .concatWith(Observable.<Integer> error(new TestException()));

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o1, o2), true).subscribe(ts);

        ts.assertError(TestException.class);
        ts.assertValues(1, 2, 3, 5, 7);
        ts.assertNotCompleted();
    }

    @Test
    public void testErrorImmediatelyDelayed() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
        Observable<Integer> o2 = Observable.<Integer> error(new TestException());

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o1, o2), true).subscribe(ts);

        ts.assertError(TestException.class);
        ts.assertNotCompleted();
        ts.assertValues(1, 3, 5, 7);
    }

    @Test
    public void testTake() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
        Observable<Integer> o2 = Observable.just(2, 4, 6, 8);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        OrderedMerge.create((Collection) Arrays.asList(o1, o2)).take(2).subscribe(ts);

        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValues(1, 2);

    }

    @Test
    public void testBackpressure() {
        Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
        Observable<Integer> o2 = Observable.just(2, 4, 6, 8);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
        OrderedMerge.create((Collection) Arrays.asList(o1, o2)).subscribe(ts);

        ts.requestMore(2);

        ts.assertNoErrors();
        ts.assertNotCompleted();
        ts.assertValues(1, 2);

    }
}