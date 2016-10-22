package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.github.davidmoten.rx.Actions;
import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.testing.TestSubscriber2;
import com.github.davidmoten.rx.testing.TestingHelper;
import com.github.davidmoten.rx.util.Pair;

import rx.Observable;
import rx.functions.Func2;

public class MatchTest {

    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(2, 1);
        match(a, b, 2, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test2() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(1, 2);
        match(a, b, 1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test3() {
        Observable<Integer> a = Observable.just(1, 2, 3);
        Observable<Integer> b = Observable.just(3, 2, 1);
        match(a, b, 3, 2, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOneMatch() {
        Observable<Integer> a = Observable.just(1);
        Observable<Integer> b = Observable.just(1);
        match(a, b, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    @Ignore
    public void testRepeats() {
        Observable<Integer> a = Observable.just(1, 1);
        Observable<Integer> b = Observable.just(1, 1);
        match(a, b, 1, 1);
    }

    private static void match(Observable<Integer> a, Observable<Integer> b, Integer... expected) {
        List<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer, Integer>>();
        for (Integer i : expected) {
            list.add(Pair.create(i, i));
        }
        TestSubscriber2<Pair<Integer, Integer>> ts = match(a, b).assertCompleted();
        assertEquals(list, ts.getOnNextEvents());
    }

    private static TestSubscriber2<Pair<Integer, Integer>> match(Observable<Integer> a,
            Observable<Integer> b) {
        return a.compose(Transformers.matchWith(b, Functions.identity(), Functions.identity(),
                new Func2<Integer, Integer, Pair<Integer, Integer>>() {
                    @Override
                    public Pair<Integer, Integer> call(Integer x, Integer y) {
                        return Pair.create(x, y);
                    }
                })).doOnNext(Actions.println()) //
                .to(TestingHelper.<Pair<Integer, Integer>> test());
    }
}
