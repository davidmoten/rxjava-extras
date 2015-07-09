package com.github.davidmoten.rx.operators;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.observers.TestSubscriber;

import com.github.davidmoten.rx.Transformers;

public class OperatorOrderedMergeTest {

    static final Func2<Integer, Integer, Integer> comparator = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer a, Integer b) {
            return a.compareTo(b);
        }
    };

    @Test
    public void testMerge() {
        Observable<Integer> o1 = Observable.just(1, 2, 4, 10);
        Observable<Integer> o2 = Observable.just(3, 5, 11);
        check(o1, o2, 1, 2, 3, 4, 5, 10, 11);
    }

    @Test
    public void testWithEmpty() {
        Observable<Integer> o1 = Observable.just(1, 2, 4, 10);
        Observable<Integer> o2 = Observable.empty();
        check(o1, o2, 1, 2, 4, 10);
    }

    private static void check(Observable<Integer> o1, Observable<Integer> o2, Integer... values) {
        List<Integer> list = o1.compose(Transformers.orderedMergeWith(o2, comparator)).toList()
                .toBlocking().single();
        assertEquals(Arrays.asList(values), list);
    }

    @Test
    public void testEmptyWithEmpty() {
        Observable<Integer> o1 = Observable.empty();
        Observable<Integer> o2 = Observable.empty();
        check(o1, o2);
    }

    @Test
    public void testOneAfterTheOther() {
        Observable<Integer> o1 = Observable.just(1, 2, 3);
        Observable<Integer> o2 = Observable.just(4, 5, 6);
        check(o1, o2, 1, 2, 3, 4, 5, 6);
    }

    @Test
    public void testOneAfterTheOtherReversed() {
        Observable<Integer> o1 = Observable.just(4, 5, 6);
        Observable<Integer> o2 = Observable.just(1, 2, 3);
        check(o1, o2, 1, 2, 3, 4, 5, 6);
    }

    @Test
    public void testSlightOverlap() {
        Observable<Integer> o1 = Observable.just(1, 3, 5);
        Observable<Integer> o2 = Observable.just(4, 6, 8).distinct();
        check(o1, o2, 1, 3, 4, 5, 6, 8);
    }

    @Test
    public void testSameValues() {
        Observable<Integer> o1 = Observable.just(1, 2, 3);
        Observable<Integer> o2 = Observable.just(1, 2, 3, 4);
        check(o1, o2, 1, 1, 2, 2, 3, 3, 4);
    }

    @Test
    public void testBeforeAndAfter() {
        Observable<Integer> o1 = Observable.just(1, 5);
        Observable<Integer> o2 = Observable.just(2, 3, 4);
        check(o1, o2, 1, 2, 3, 4, 5);
    }

    @Test
    public void testSpecialcase() {
        Observable<Integer> o1 = Observable.just(1, 10);
        Observable<Integer> o2 = Observable.just(3, 4, 5, 6);
        check(o1, o2, 1, 3, 4, 5, 6, 10);
    }

    @Test
    public void testOneByOneBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1);
        Observable<Integer> o1 = Observable.just(1, 2, 4, 10);
        Observable<Integer> o2 = Observable.just(3, 5, 11);
        o1.doOnRequest(printRequest("upstream"))
                .compose(Transformers.orderedMergeWith(o2, comparator))
                .doOnRequest(printRequest("justUp")).subscribe(ts);
        ts.assertNotCompleted();
        ts.assertValues(1);
        ts.requestMore(1);
        ts.assertNotCompleted();
        ts.assertValues(1, 2);
        ts.requestMore(2);
        ts.assertNotCompleted();
        ts.assertValues(1, 2, 3, 4);
        ts.requestMore(1);
        ts.assertNotCompleted();
        ts.assertValues(1, 2, 3, 4, 5);
    }

    private Action1<Long> printRequest(final String label) {
        return new Action1<Long>() {

            @Override
            public void call(Long t) {
                System.out.println(label + "=" + t);
            }
        };
    }
}
