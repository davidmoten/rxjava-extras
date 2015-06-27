package com.github.davidmoten.rx;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;

import rx.Observable;

public class Benchmarks {

    private static final int MANY = 1000000;

    @Benchmark
    public void takeLastOneFromRxJavaLibraryMany() {
        Observable.range(1, MANY).takeLast(1).subscribe();
    }

    @Benchmark
    public void addAndIterateArrayList() {
        addAndIterateList(new ArrayList<Integer>());
    }

    @Benchmark
    public void addAndIterateLinkedList() {
        addAndIterateList(new LinkedList<Integer>());
    }

    private void addAndIterateList(List<Integer> list) {
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        for (Integer element : list) {
        }
        list.size();
    }

}
