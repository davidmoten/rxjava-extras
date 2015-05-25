package com.github.davidmoten.rx;

import rx.Scheduler;
import rx.schedulers.Schedulers;

import com.github.davidmoten.rx.operators.OperatorBufferRequests;
import com.github.davidmoten.rx.operators.OperatorBufferRequests.Bias;

public final class Operators {

    public static <T> OperatorBufferRequests<T> bufferRequestsSyncBias(int capacity) {
        return new OperatorBufferRequests<T>(capacity, Bias.SYNC_BIAS);
    }
    
    public static <T> OperatorBufferRequests<T> bufferRequestsAsyncBias(int capacity) {
        return bufferRequestsAsyncBias(capacity, Schedulers.computation());
    }
    
    public static <T> OperatorBufferRequests<T> bufferRequestsAsyncBias(int capacity, Scheduler scheduler) {
        return new OperatorBufferRequests<T>(capacity, Bias.ASYNC_BIAS, scheduler);
    }
}
