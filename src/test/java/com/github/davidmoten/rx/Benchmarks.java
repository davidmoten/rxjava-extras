package com.github.davidmoten.rx;

import org.openjdk.jmh.annotations.Benchmark;

import rx.Observable;
import rx.functions.Func2;

import com.github.davidmoten.rx.operators.OperatorLast;

public class Benchmarks {

    private static final int MANY = 1000000;
    private static final int FEW = 5;

    @Benchmark
    public void lastFromRxJavaLibrary() {
        Observable.range(1, MANY).last().subscribe();
    }

    @Benchmark
    public void lastFromRxJavaLibraryFew() {
        Observable.range(1, FEW).last().subscribe();
    }

    @Benchmark
    public void lastFromExtras() {
        Observable.range(1, MANY).lift(OperatorLast.<Integer> create()).subscribe();
    }

    @Benchmark
    public void lastFromExtrasFew() {
        Observable.range(1, FEW).lift(OperatorLast.<Integer> create()).subscribe();
    }

    private static final Func2<Integer, ? super Integer, Integer> COUNT = new Func2<Integer, Integer, Integer>() {

        @Override
        public Integer call(Integer count, Integer o) {
            return count + 1;
        }
    };

}
