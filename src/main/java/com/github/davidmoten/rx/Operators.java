package com.github.davidmoten.rx;

import rx.Scheduler;
import rx.schedulers.Schedulers;

import com.github.davidmoten.rx.operators.OperatorBufferRequests;

public final class Operators {

    public static <T> OperatorBufferRequests<T> bufferRequests() {
        return new OperatorBufferRequests<T>();
    }
    
    public static <T> OperatorBufferRequests<T> bufferRequestsAsyncOptimized() {
        return bufferRequestsAsyncOptimized(Schedulers.computation());
    }
    
    public static <T> OperatorBufferRequests<T> bufferRequestsAsyncOptimized(Scheduler scheduler) {
        return new OperatorBufferRequests<T>(scheduler);
    }
    
}
