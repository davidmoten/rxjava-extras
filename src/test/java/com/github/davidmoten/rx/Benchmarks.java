package com.github.davidmoten.rx;

import org.openjdk.jmh.annotations.Benchmark;

import rx.Observable;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import com.github.davidmoten.rx.operators.OperatorLast;
import com.github.davidmoten.rx.operators.OperatorReduce;

public class Benchmarks {

    private static final int MANY = 1000000;
    private static final int FEW = 5;

    @Benchmark
    public void reduceManyFromRxJavaLibrary() {
        Observable.range(1, MANY).reduce(0, COUNT).subscribe();
    }

    @Benchmark
    public void reduceManyFromExtras() {
        Observable.range(1, MANY).map(Functions.<Integer> identity())
                .lift(OperatorReduce.create(0, COUNT)).subscribe();
    }

    @Benchmark
    public void reduceManyFromRxJavaLibraryAsync() {
        Observable.range(1, MANY).reduce(0, COUNT).observeOn(Schedulers.computation()).toBlocking()
                .single();
    }

    @Benchmark
    public void reduceManyFromExtrasAsync() {
        Observable.range(1, MANY).lift(OperatorReduce.create(0, COUNT))
                .observeOn(Schedulers.computation()).toBlocking().single();
    }

    @Benchmark
    public void reduceFewFromRxJavaLibrary() {
        Observable.range(1, FEW).reduce(0, COUNT).subscribe();
    }

    @Benchmark
    public void reduceFewFromExtras() {
        Observable.range(1, FEW).lift(OperatorReduce.create(0, COUNT)).subscribe();
    }

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

    public static void main(String[] args) {
        while (true) {
            Observable.range(1, MANY).lift(OperatorReduce.create(0, COUNT)).subscribe();
        }
    }
}
