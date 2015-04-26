package com.github.davidmoten.rx;

import org.openjdk.jmh.annotations.Benchmark;

import rx.Observable;

public class Benchmarks {

    private static final int MANY = 1000000;

    @Benchmark
    public void takeLastOneFromRxJavaLibraryMany() {
        Observable.range(1, MANY).takeLast(1).subscribe();
    }

}
