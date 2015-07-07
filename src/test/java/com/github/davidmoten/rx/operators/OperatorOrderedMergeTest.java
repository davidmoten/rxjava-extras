package com.github.davidmoten.rx.operators;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func2;

import com.github.davidmoten.rx.Transformers;

public class OperatorOrderedMergeTest {

    private static final Func2<Integer, Integer, Integer> comparator = new Func2<Integer, Integer, Integer>() {
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
        List<Integer> list = o1.compose(Transformers.mergeOrderedWith(o2, comparator)).toList()
                .toBlocking().single();
        System.out.println(list);
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
}
