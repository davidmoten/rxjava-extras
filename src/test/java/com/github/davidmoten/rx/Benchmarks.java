package com.github.davidmoten.rx;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import com.github.davidmoten.rx.buffertofile.DataSerializers;
import com.github.davidmoten.rx.perf.LatchedObserver;

import rx.Observable;
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

    @Benchmark
    public void perfStringSplit() {
        Observable.from(Arrays //
                .asList("the quick brown ", "fox jumped over", " the lazy", " dog")) //
                .compose(Transformers.split("o"));
    }

}
