package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.testing.TestSubscriber2;
import com.github.davidmoten.rx.testing.TestingHelper;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

public class OnSubscribeMatchTest {

    @Test
    public void test() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(2, 1);
        match(a, b, 2, 1);
    }

    @Test
    public void test2() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(1, 2);
        match(a, b, 1, 2);
    }

    @Test
    public void test3() {
        Observable<Integer> a = Observable.just(1, 2, 3);
        Observable<Integer> b = Observable.just(3, 2, 1);
        match(a, b, 3, 2, 1);
    }

    @Test
    public void testOneMatch() {
        Observable<Integer> a = Observable.just(1);
        Observable<Integer> b = Observable.just(1);
        match(a, b, 1);
    }

    @Test
    public void testEmpties() {
        Observable<Integer> a = Observable.empty();
        Observable<Integer> b = Observable.empty();
        match(a, b);
    }

    @Test
    public void testRepeats() {
        Observable<Integer> a = Observable.just(1, 1);
        Observable<Integer> b = Observable.just(1, 1);
        match(a, b, 1, 1);
    }

    @Test
    public void testRepeats2() {
        Observable<Integer> a = Observable.just(1, 1, 2, 3, 1);
        Observable<Integer> b = Observable.just(1, 2, 1, 3, 1);
        match(a, b, 1, 2, 1, 3, 1);
    }

    @Test
    public void testNoMatchExistsForAtLeastOneFirstLonger() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(1);
        match(a, b, 1);
    }

    @Test
    public void testNoMatchExistsForAtLeastOneSameLength() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(1, 3);
        match(a, b, 1);
    }

    @Test
    public void testNoMatchExistsForAtLeastOneSecondLonger() {
        Observable<Integer> a = Observable.just(1);
        Observable<Integer> b = Observable.just(1, 2);
        match(a, b, 1);
    }

    @Test
    public void testNoMatchExistsAtAll() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(3, 4);
        match(a, b, new Integer[] {});
    }
    
    @Test
    public void testUnsubscribe() {
        Observable<Integer> a = Observable.just(1, 2, 3, 4);
        Observable<Integer> b = Observable.just(3, 2, 1, 4);
    }
    
    
    private static Observable<Integer> matchThem(Observable<Integer> a, Observable<Integer> b) {
        return a.compose(Transformers.matchWith(b, Functions.identity(),
                Functions.identity(), COMBINER));
    }
    
    

    @Test
    public void testLongReversed() {
        final int n = 500;
        Observable<Integer> a = Observable.range(1, n).map(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer x) {
                return n + 1 - x;
            }
        });
        Observable<Integer> b = Observable.range(1, n);
        boolean equals = Observable
                .sequenceEqual(a.compose(Transformers.matchWith(b, Functions.identity(),
                        Functions.identity(), COMBINER)).sorted(), Observable.range(1, n))
                .toBlocking().single();
        assertTrue(equals);
    }

    @Test
    public void testLongShifted() {
        final int n = 100000;
        Observable<Integer> a = Observable.just(0).concatWith(Observable.range(1, n));
        Observable<Integer> b = Observable.range(1, n);
        assertTrue(Observable
                .sequenceEqual(a.compose(Transformers.matchWith(b, Functions.identity(),
                        Functions.identity(), COMBINER)), Observable.range(1, n))
                .toBlocking().single());
    }

    private static void match(Observable<Integer> a, Observable<Integer> b, Integer... expected) {
        List<Integer> list = Arrays.asList(expected);
        TestSubscriber2<Integer> ts = match(a, b).assertCompleted();
        assertEquals(list, ts.getOnNextEvents());
    }

    private static final Func2<Integer, Integer, Integer> COMBINER = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer x, Integer y) {
            return x;
        }
    };

    private static TestSubscriber2<Integer> match(Observable<Integer> a, Observable<Integer> b) {
        return a.compose(
                Transformers.matchWith(b, Functions.identity(), Functions.identity(), COMBINER))
                .to(TestingHelper.<Integer> test());
    }
}
