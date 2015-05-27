package com.github.davidmoten.rx;

import rx.Scheduler;
import rx.schedulers.Schedulers;

import com.github.davidmoten.rx.operators.OperatorBufferEmissions;

public final class Operators {

    public static <T> OperatorBufferEmissions<T> bufferEmissions() {
        return new OperatorBufferEmissions<T>();
    }
    
    public static <T> OperatorBufferEmissions<T> bufferEmissionsAsyncOptimized() {
        return bufferEmissionsAsyncOptimized(Schedulers.computation());
    }
    
    public static <T> OperatorBufferEmissions<T> bufferEmissionsAsyncOptimized(Scheduler scheduler) {
        return new OperatorBufferEmissions<T>(scheduler);
    }
    
}
