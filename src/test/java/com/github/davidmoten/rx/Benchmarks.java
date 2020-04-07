package com.github.davidmoten.rx;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import com.github.davidmoten.rx.buffertofile.DataSerializers;
import com.github.davidmoten.rx.perf.LatchedObserver;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class Benchmarks {

    // @Benchmark
    public void perfOnBackpressureBufferToFileFor100_000IntegersOnComputation(Blackhole bh)
            throws InterruptedException {
        LatchedObserver<Integer> observer = new LatchedObserver<Integer>(bh);
        Observable.range(1, 100000)
                .compose(Transformers.onBackpressureBufferToFile(DataSerializers.integer()))
                .subscribe(observer);
        observer.latch.await(100, TimeUnit.SECONDS);
    }

    // @Benchmark
    public void perfOnBackpressureBufferToFileFor100_000IntegersSychronous(Blackhole bh)
            throws InterruptedException {
        LatchedObserver<Integer> observer = new LatchedObserver<Integer>(bh);
        Observable.range(1, 100000).compose(Transformers
                .onBackpressureBufferToFile(DataSerializers.integer(), Schedulers.immediate()))
                .subscribe(observer);
        observer.latch.await(100, TimeUnit.SECONDS);
    }

    // @Benchmark
    public void perfOnBackpressureBufferToFileFor3000_1KMessagesOnComputation(Blackhole bh)
            throws InterruptedException {
        LatchedObserver<byte[]> observer = new LatchedObserver<byte[]>(bh);
        Observable.range(1, 3000).map(new Func1<Integer, byte[]>() {
            @Override
            public byte[] call(Integer n) {
                return new byte[1000];
            }
        }).compose(Transformers.onBackpressureBufferToFile(DataSerializers.byteArray(),
                Schedulers.immediate())).subscribe(observer);
        observer.latch.await(100, TimeUnit.SECONDS);
    }

    // @Benchmark
    public void perfOnBackpressureBufferToFileFor3000_1KMessagesOnSynchronous(Blackhole bh)
            throws InterruptedException {
        LatchedObserver<byte[]> observer = new LatchedObserver<byte[]>(bh);
        Observable.range(1, 3000).map(new Func1<Integer, byte[]>() {
            @Override
            public byte[] call(Integer n) {
                return new byte[1000];
            }
        }).compose(Transformers.onBackpressureBufferToFile(DataSerializers.byteArray(),
                Schedulers.immediate())).subscribe(observer);
        observer.latch.await(100, TimeUnit.SECONDS);
    }

    private static List<String> lines = readLines();

    private static List<String> readLines() {
        try {
            return Files.lines(new File("src/test/resources/the-black-gang.txt").toPath()) //
                    .collect(Collectors.<String>toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    @Benchmark
    public void perfStringSplit(final Blackhole bh) {
        Observable //
                .from(lines) //
                .compose(Transformers.split("o")) //
                .forEach(new Action1<String>() {
                    @Override
                    public void call(String x) {
                        bh.consume(x);
                    }
                });
    }

    @Benchmark
    public void perfStringSplitWithLimit(final Blackhole bh) {
        // this should show the allocation overhead of the State object (almost zero effect)
        Observable //
                .from(lines) //
                .compose(Transformers.split(100, "o", 1)) //
                .forEach(new Action1<String>() {
                    @Override
                    public void call(String x) {
                        bh.consume(x);
                    }
                });
    }

}
