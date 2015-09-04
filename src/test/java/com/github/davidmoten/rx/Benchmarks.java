package com.github.davidmoten.rx;

import org.openjdk.jmh.annotations.Benchmark;

import rx.Observable;
import rx.Subscriber;

public class Benchmarks {

    private static final Observable<Integer> o1 = Observable.just(1);
    private static final Observable<Integer> o5 = Observable.just(1, 2, 3, 4, 5);
    private static final Observable<Integer> o1000 = Observable.just(1);

    @Benchmark
    public void concatOne() {
        concat(o1);
    }

    @Benchmark
    public void mergeOne() {
        merge(o1);
    }

    @Benchmark
    public void concatSome() {
        concat(o5);
    }

    @Benchmark
    public void mergeSome() {
        merge(o5);
    }

    @Benchmark
    public void concatMany() {
        concat(o1000);
    }

    @Benchmark
    public void mergeMany() {
        merge(o1000);
    }

    private void concat(Observable<Integer> o) {
        MySubscriber<Integer> subscriber = new MySubscriber<Integer>();
        o.concatWith(o).subscribe(subscriber);
        if (subscriber.count == 0) {
            throw new RuntimeException();
        }
    }

    private void merge(Observable<Integer> o) {
        MySubscriber<Integer> subscriber = new MySubscriber<Integer>();
        o.mergeWith(o).subscribe(subscriber);
        if (subscriber.count == 0) {
            throw new RuntimeException();
        }
    }

    private static class MySubscriber<T> extends Subscriber<T> {
        int count;

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(T t) {
            count++;
        }
    };

}
