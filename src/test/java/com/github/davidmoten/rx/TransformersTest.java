package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.Observable.Transformer;
import rx.Observer;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func3;
import rx.observers.TestSubscriber;

import com.github.davidmoten.rx.util.Pair;

public class TransformersTest {

    @Test
    public void testStatisticsOnEmptyStream() {
        Observable<Integer> nums = Observable.empty();
        Statistics s = nums.compose(Transformers.collectStats()).last().toBlocking().single();
        assertEquals(0, s.count());
        assertEquals(0, s.sum(), 0.0001);
        assertTrue(Double.isNaN(s.mean()));
        assertTrue(Double.isNaN(s.sd()));
    }

    @Test
    public void testStatisticsOnSingleElement() {
        Observable<Integer> nums = Observable.just(1);
        Statistics s = nums.compose(Transformers.collectStats()).last().toBlocking().single();
        assertEquals(1, s.count());
        assertEquals(1, s.sum(), 0.0001);
        assertEquals(1.0, s.mean(), 0.00001);
        assertEquals(0, s.sd(), 0.00001);
    }

    @Test
    public void testStatisticsOnMultipleElements() {
        Observable<Integer> nums = Observable.just(1, 4, 10, 20);
        Statistics s = nums.compose(Transformers.collectStats()).last().toBlocking().single();
        assertEquals(4, s.count());
        assertEquals(35.0, s.sum(), 0.0001);
        assertEquals(8.75, s.mean(), 0.00001);
        assertEquals(7.258615570478987, s.sd(), 0.00001);
    }

    @Test
    public void testStatisticsPairOnEmptyStream() {
        Observable<Integer> nums = Observable.empty();
        boolean isEmpty = nums.compose(Transformers.collectStats(Functions.<Integer> identity()))
                .isEmpty().toBlocking().single();
        assertTrue(isEmpty);
    }

    @Test
    public void testStatisticsPairOnSingleElement() {
        Observable<Integer> nums = Observable.just(1);
        Pair<Integer, Statistics> s = nums
                .compose(Transformers.collectStats(Functions.<Integer> identity())).last()
                .toBlocking().single();
        assertEquals(1, (int) s.a());
        assertEquals(1, s.b().count());
        assertEquals(1, s.b().sum(), 0.0001);
        assertEquals(1.0, s.b().mean(), 0.00001);
        assertEquals(0, s.b().sd(), 0.00001);
    }

    @Test
    public void testStatisticsPairOnMultipleElements() {
        Observable<Integer> nums = Observable.just(1, 4, 10, 20);
        Pair<Integer, Statistics> s = nums
                .compose(Transformers.collectStats(Functions.<Integer> identity())).last()
                .toBlocking().single();
        assertEquals(4, s.b().count());
        assertEquals(35.0, s.b().sum(), 0.0001);
        assertEquals(8.75, s.b().mean(), 0.00001);
        assertEquals(7.258615570478987, s.b().sd(), 0.00001);
    }

    @Test
    public void testSort() {
        Observable<Integer> o = Observable.just(5, 3, 1, 4, 2);
        List<Integer> list = o.compose(Transformers.<Integer> sort()).toList().toBlocking()
                .single();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void testSortWithComparator() {
        Observable<Integer> o = Observable.just(5, 3, 1, 4, 2);
        List<Integer> list = o.compose(Transformers.<Integer> sort(new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        })).toList().toBlocking().single();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), list);
    }

    @Test
    public void testStateTransitionThrowsError() {
        final RuntimeException ex = new RuntimeException("boo");
        Func0<Integer> initialState = new Func0<Integer>() {

            @Override
            public Integer call() {
                return 1;
            }
        };
        Func3<Integer, Integer, Observer<Integer>, Integer> transition = new Func3<Integer, Integer, Observer<Integer>, Integer>() {

            @Override
            public Integer call(Integer collection, Integer t, Observer<Integer> observer) {
                throw ex;
            }

        };
        Action2<Integer, Observer<Integer>> completionAction = new Action2<Integer, Observer<Integer>>() {
            @Override
            public void call(Integer collection, Observer<Integer> observer) {
            }
        };
        Transformer<Integer, Integer> transformer = Transformers.stateMachine(initialState,
                transition, completionAction);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.just(1, 1, 1).compose(transformer).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertError(ex);
    }
}
