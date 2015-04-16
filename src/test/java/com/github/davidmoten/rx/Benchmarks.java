package com.github.davidmoten.rx;

import org.openjdk.jmh.annotations.Benchmark;

import rx.Observable;
import rx.functions.Func2;

import com.github.davidmoten.rx.operators.OperatorTakeLastOne;

public class Benchmarks {

    private static final int MANY = 1000000;
    private static final int SOME = 100;
    private static final int FEW = 5;

    @Benchmark
    public void takeLastOneFromRxJavaLibraryMany() {
        Observable.range(1, MANY).takeLast(1).subscribe();
    }

    @Benchmark
    public void takeLastOneFromRxJavaLibraryFew() {
        Observable.range(1, FEW).takeLast(1).subscribe();
    }

    @Benchmark
    public void takeLastOneFromRxJavaLibrarySome() {
        Observable.range(1, SOME).takeLast(1).subscribe();
    }

    @Benchmark
    public void takeLastOneNewMany() {
        Observable.range(1, MANY).lift(OperatorTakeLastOne.<Integer> create()).subscribe();
    }

    @Benchmark
    public void takeLastOneNewFew() {
        Observable.range(1, FEW).lift(OperatorTakeLastOne.<Integer> create()).subscribe();
    }

    @Benchmark
    public void takeLastOneNewSome() {
        Observable.range(1, SOME).lift(OperatorTakeLastOne.<Integer> create()).subscribe();
    }

    private static final Func2<Integer, ? super Integer, Integer> COUNT = new Func2<Integer, Integer, Integer>() {

        @Override
        public Integer call(Integer count, Integer o) {
            return count + 1;
        }
    };

}
