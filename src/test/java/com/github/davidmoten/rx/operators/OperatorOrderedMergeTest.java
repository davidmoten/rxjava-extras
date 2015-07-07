package com.github.davidmoten.rx.operators;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func2;

import com.github.davidmoten.rx.Transformers;

public class OperatorOrderedMergeTest {

    @Test
    public void test() {
        List<Integer> list = Observable
                .just(1, 2, 4, 10)
                .compose(
                        Transformers.mergeOrderedWith(Observable.just(3, 5, 11),
                                new Func2<Integer, Integer, Integer>() {
                                    @Override
                                    public Integer call(Integer a, Integer b) {
                                        return a.compareTo(b);
                                    }
                                })).toList().toBlocking().single();
        System.out.println(list);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 10, 11), list);
    }
}
