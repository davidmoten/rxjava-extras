package com.github.davidmoten.rx;

import org.openjdk.jmh.annotations.Benchmark;

import rx.Observable;
import rx.functions.Func2;

import com.github.davidmoten.rx.operators.OperatorLast;

public class Benchmarks {

    private static final int MANY = 1000000;
    private static final int FEW = 5;

    @Benchmark
    public void scanFromRxJavaLibrary() {
        Observable.range(1, MANY).scan(0, COUNT).last().subscribe();
    }

    @Benchmark
    public void scanFromExtras() {
        Observable.range(1, MANY).scan(0, COUNT).lift(OperatorLast.<Integer> create()).subscribe();
    }

    private static final Func2<Integer, ? super Integer, Integer> COUNT = new Func2<Integer, Integer, Integer>() {

        @Override
        public Integer call(Integer count, Integer o) {
            return count + 1;
        }
    };

}
