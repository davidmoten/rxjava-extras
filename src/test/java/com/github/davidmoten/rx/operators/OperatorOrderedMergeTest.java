package com.github.davidmoten.rx.operators;

import static org.junit.Assert.assertEquals;
import static rx.Observable.from;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func2;

import com.github.davidmoten.rx.Transformers;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class OperatorOrderedMergeTest {

    private static final Func2<Integer, Integer, Integer> comparator = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer a, Integer b) {
            return a.compareTo(b);
        }
    };

    @Test
    public void testMerge() {
        // hang on to one stand-alone test like this so we can customize for
        // failure cases arriving out of testWithAllCombinationsFromPowerSet
        Observable<Integer> o1 = Observable.just(1, 2, 4, 10);
        Observable<Integer> o2 = Observable.just(3, 5, 11);
        check(o1, o2, 1, 2, 3, 4, 5, 10, 11);
    }

    @Test
    public void testWithAllCombinationsFromPowerSet() {
        // this test covers everything!
        for (int n = 0; n <= 10; n++) {
            Set<Integer> numbers = Sets.newTreeSet();
            for (int i = 1; i <= n; i++) {
                numbers.add(i);
            }
            for (Set<Integer> a : Sets.powerSet(numbers)) {
                TreeSet<Integer> x = Sets.newTreeSet(a);
                TreeSet<Integer> y = Sets.newTreeSet(Sets.difference(numbers, x));
                Observable<Integer> o1 = from(x);
                Observable<Integer> o2 = from(y);
                List<Integer> list = o1.compose(Transformers.orderedMergeWith(o2, comparator))
                        .toList().toBlocking().single();
                // System.out.println(x + "   " + y);
                assertEquals(Lists.newArrayList(numbers), list);
            }
        }
    }

    private static void check(Observable<Integer> o1, Observable<Integer> o2, Integer... values) {
        List<Integer> list = o1.compose(Transformers.orderedMergeWith(o2, comparator)).toList()
                .toBlocking().single();
        assertEquals(Arrays.asList(values), list);
    }
}
