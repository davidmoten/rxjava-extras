package com.github.davidmoten.rx;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

import com.github.davidmoten.rx.buffertofile.DataSerializers;
import com.github.davidmoten.rx.perf.LatchedObserver;

import rx.Observable;

public class Benchmarks {

	@Benchmark
	public void perfOnBackpressureBufferToFileForIntegersOnComputation(Blackhole bh) throws InterruptedException {
		LatchedObserver<Integer> observer = new LatchedObserver<Integer>(bh);
		Observable.range(1, 1000).compose(Transformers.onBackpressureBufferToFile(DataSerializers.integer()))
				.subscribe(observer);
		observer.latch.await(10, TimeUnit.SECONDS);
	}

}
