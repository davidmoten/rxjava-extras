package com.github.davidmoten.rx;

import org.openjdk.jmh.annotations.Benchmark;

import rx.Observable;
import rx.functions.Func2;

import com.github.davidmoten.rx.operators.OperatorReduce;

public class Benchmarks {

    @Benchmark
    public void reduceManyFromRxJavaLibrary() {
        Observable.range(1, 1000000).reduce(0, COUNT).subscribe();
    }

    @Benchmark
    public void reduceManyFromExtras() {
        Observable.range(1, 1000000).lift(OperatorReduce.create(0, COUNT)).subscribe();
    }

    @Benchmark
    public void reduceFewFromRxJavaLibrary() {
        Observable.range(1, 5).reduce(0, COUNT).subscribe();
    }

    @Benchmark
    public void reduceFewFromExtras() {
        Observable.range(1, 5).lift(OperatorReduce.create(0, COUNT)).subscribe();
    }

    private static final Func2<Integer, ? super Integer, Integer> COUNT = new Func2<Integer, Integer, Integer>() {

        @Override
        public Integer call(Integer count, Integer o) {
            return count + 1;
        }
    };
}
