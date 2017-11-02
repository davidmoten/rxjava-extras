package com.github.davidmoten.rx.internal.operators;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.github.davidmoten.rx.Actions;
import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.StateMachine.Transition;
import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.util.BackpressureUtils;

import rx.Observable;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.observers.TestSubscriber;

public class TransformerStateMachineTest {

    @Test
    public void testStateTransitionThrowsError() {
        final RuntimeException ex = new RuntimeException("boo");
        Func0<Integer> initialState = Functions.constant0(1);
        Func3<Integer, Integer, Observer<Integer>, Integer> transition = new Func3<Integer, Integer, Observer<Integer>, Integer>() {

            @Override
            public Integer call(Integer collection, Integer t, Observer<Integer> observer) {
                throw ex;
            }

        };
        Func2<Integer, Observer<Integer>, Boolean> completion = Functions.alwaysTrue2();
        Transformer<Integer, Integer> transformer = Transformers.stateMachine(initialState,
                transition, completion);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.just(1, 1, 1).compose(transformer).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertError(ex);
    }

    @Test
    public void testCompletionActionThrowsError() {
        final RuntimeException ex = new RuntimeException("boo");
        Func0<Integer> initialState = Functions.constant0(1);
        Func3<Integer, Integer, Observer<Integer>, Integer> transition = new Func3<Integer, Integer, Observer<Integer>, Integer>() {

            @Override
            public Integer call(Integer collection, Integer t, Observer<Integer> observer) {
                return t;
            }

        };
        Func2<Integer, Observer<Integer>, Boolean> completion = new Func2<Integer, Observer<Integer>, Boolean>() {
            @Override
            public Boolean call(Integer collection, Observer<Integer> observer) {
                throw ex;
            }
        };
        Transformer<Integer, Integer> transformer = Transformers.stateMachine(initialState,
                transition, completion);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.just(1, 1, 1).compose(transformer).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertError(ex);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToListUntilChanged() {
        Observable<Integer> o = Observable.just(1, 1, 1, 2, 2, 3);
        List<List<Integer>> lists = o.compose(Transformers.<Integer> toListUntilChanged())
                // get as list
                .toList().toBlocking().single();
        assertEquals(asList(asList(1, 1, 1), asList(2, 2), asList(3)), lists);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToListUntilChangedMultipleAtEnd() {
        Observable<Integer> o = Observable.just(1, 1, 1, 2, 2, 3, 3);
        List<List<Integer>> lists = o.compose(Transformers.<Integer> toListUntilChanged())
                // get as list
                .toList().toBlocking().single();
        assertEquals(asList(asList(1, 1, 1), asList(2, 2), asList(3, 3)), lists);
    }

    @Test
    public void testToListUntilChangedWithEmpty() {
        Observable<Integer> o = Observable.empty();
        List<List<Integer>> lists = o.compose(Transformers.<Integer> toListUntilChanged())
                // get as list
                .toList().toBlocking().single();
        assertTrue(lists.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToListUntilChangedWithNoChange() {
        Observable<Integer> o = Observable.just(1, 1, 1);
        List<List<Integer>> lists = o.compose(Transformers.<Integer> toListUntilChanged())
                // get as list
                .toList().toBlocking().single();
        assertEquals(asList(asList(1, 1, 1)), lists);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToListUntilChangedWithOnlyChange() {
        Observable<Integer> o = Observable.just(1, 2, 3);
        List<List<Integer>> lists = o.compose(Transformers.<Integer> toListUntilChanged())
                // get as list
                .toList().toBlocking().single();
        assertEquals(asList(asList(1), asList(2), asList(3)), lists);
    }

    @Test
    public void testToListUntilChangedWithError() {
        Exception ex = new Exception("boo");
        Observable<Integer> o = Observable.error(ex);
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        o.compose(Transformers.<Integer> toListUntilChanged()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertError(ex);
        ts.assertUnsubscribed();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToListWithTemperatures() {
        List<List<Integer>> lists = Observable.just(10, 5, 2, -1, -2, -5, -1, 2, 5, 6)
                .compose(Transformers.toListWhile(new Func2<List<Integer>, Integer, Boolean>() {
                    @Override
                    public Boolean call(List<Integer> list, Integer t) {
                        return list.isEmpty() || Math.signum(list.get(0)) < 0 && Math.signum(t) < 0
                                || Math.signum(list.get(0)) >= 0 && Math.signum(t) >= 0;
                    }
                })).toList().toBlocking().single();
        assertEquals(asList(asList(10, 5, 2), asList(-1, -2, -5, -1), asList(2, 5, 6)), lists);
    }

    @Test
    public void testUnsubscriptionFromTransition() {
        Func0<Integer> initialState = Functions.constant0(1);
        Func3<Integer, Integer, Subscriber<Integer>, Integer> transition = new Func3<Integer, Integer, Subscriber<Integer>, Integer>() {

            @Override
            public Integer call(Integer collection, Integer t, Subscriber<Integer> subscriber) {
                subscriber.onNext(123);
                subscriber.unsubscribe();
                return 1;
            }

        };
        Func2<Integer, Observer<Integer>, Boolean> completion = Functions.alwaysTrue2();
        Transformer<Integer, Integer> transformer = Transformers.stateMachine(initialState,
                transition, completion);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.just(1, 1, 1).repeat().compose(transformer).subscribe(ts);
        ts.assertValue(123);
        ts.assertCompleted();
        ts.assertUnsubscribed();
    }

    @Test
    public void testCompletionReturnsFalse() {
        Func0<Integer> initialState = Functions.constant0(1);
        Func3<Integer, Integer, Subscriber<Integer>, Integer> transition = new Func3<Integer, Integer, Subscriber<Integer>, Integer>() {

            @Override
            public Integer call(Integer collection, Integer t, Subscriber<Integer> subscriber) {
                subscriber.onNext(123);
                return 1;
            }

        };
        Func2<Integer, Subscriber<Integer>, Boolean> completion = new Func2<Integer, Subscriber<Integer>, Boolean>() {
            @Override
            public Boolean call(Integer collection, Subscriber<Integer> subscriber) {
                subscriber.onNext(456);
                return false;
            }
        };
        Transformer<Integer, Integer> transformer = Transformers.stateMachine(initialState,
                transition, completion);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.just(1).compose(transformer).subscribe(ts);
        ts.assertValues(123, 456);
        ts.assertNotCompleted();
    }

    @Test
    public void testUnsubscribeJustBeforeCompletion() {
        Func0<Integer> initialState = Functions.constant0(1);
        Func3<Integer, Integer, Subscriber<Integer>, Integer> transition = new Func3<Integer, Integer, Subscriber<Integer>, Integer>() {

            @Override
            public Integer call(Integer collection, Integer t, Subscriber<Integer> subscriber) {
                subscriber.onNext(123);
                return 1;
            }

        };
        Func2<Integer, Subscriber<Integer>, Boolean> completion = new Func2<Integer, Subscriber<Integer>, Boolean>() {
            @Override
            public Boolean call(Integer collection, Subscriber<Integer> subscriber) {
                subscriber.onNext(456);
                subscriber.unsubscribe();
                return true;
            }
        };
        Transformer<Integer, Integer> transformer = Transformers.stateMachine(initialState,
                transition, completion);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.just(1).compose(transformer).subscribe(ts);
        ts.assertValues(123, 456);
        ts.assertNotCompleted();
    }

    @Test
    public void testForMemoryLeaks() {
        int n = 1000000;
        int count = Observable.range(1, n)
//                .lift(Logging.<Integer> logger().showCount().showMemory().every(1, TimeUnit.SECONDS)
//                        .log())
                .compose(Transformers.toListWhile(new Func2<List<Integer>, Integer, Boolean>() {
                    @Override
                    public Boolean call(List<Integer> list, Integer t) {
                        return list.size() == 0;
                    }
                })).count().toBlocking().single();
        assertEquals(n, count);
    }

    @Test
    public void testForCompletionWithinStateMachine() {
        Func0<Integer> initialState = Functions.constant0(1);
        Func3<Integer, Integer, Subscriber<Integer>, Integer> transition = new Func3<Integer, Integer, Subscriber<Integer>, Integer>() {

            @Override
            public Integer call(Integer collection, Integer t, Subscriber<Integer> subscriber) {
                subscriber.onNext(123);
                // complete from within transition
                subscriber.onCompleted();
                return 1;
            }

        };
        Func2<? super Integer, ? super Subscriber<Integer>, Boolean> completion = Functions
                .alwaysTrue2();
        Transformer<Integer, Integer> transformer = Transformers.stateMachine(initialState,
                transition, completion);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final AtomicInteger count = new AtomicInteger(0);
        Observable.just(1, 2, 3).doOnNext(Actions.increment1(count)).compose(transformer)
                .subscribe(ts);
        ts.assertValues(123);
        ts.assertCompleted();
        assertEquals(1, count.get());
    }

    @Test
    public void testDoesNotRequestMaxValueOfUpstreamIfBackpressureBufferOptionSelected() {
        final AtomicLong requested = new AtomicLong();
        Observable<Integer> o = Observable.range(1, 200) //
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long n) {
                        BackpressureUtils.getAndAddRequest(requested, n);
                    }
                });
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        o //
                .compose(Transformers.stateMachine().initialState(null)
                        .transition(new Transition<Object, Integer, Integer>() {

                            @Override
                            public Object call(Object state, Integer value,
                                    Subscriber<Integer> subscriber) {
                                // subscriber.onNext(value);
                                return state;
                            }
                        }).build())
                // get as list
                .subscribe(ts);
        ts.requestMore(4);
        assertEquals(201, requested.get());
        ts.assertCompleted();
        ts.assertNoValues();
    }

}