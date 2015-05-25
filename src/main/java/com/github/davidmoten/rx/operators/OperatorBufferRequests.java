package com.github.davidmoten.rx.operators;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.Operator;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.internal.util.unsafe.SpscArrayQueue;

import com.github.davidmoten.util.Drainer;
import com.github.davidmoten.util.DrainerAsyncBiased;
import com.github.davidmoten.util.DrainerSyncBiased;

public class OperatorBufferRequests<T> implements Operator<T, T> {

    public static enum Bias {
        ASYNC_BIAS, SYNC_BIAS;
    }

    private final int capacity;
    private final Bias bias;
    private final Scheduler scheduler;

    public OperatorBufferRequests(int capacity, Bias bias) {
        this(capacity, bias, null);
    }
    
    public OperatorBufferRequests(int capacity, Bias bias, Scheduler scheduler) {
        this.capacity = capacity;
        this.bias = bias;
        if (bias == Bias.SYNC_BIAS && scheduler != null) {
            throw new IllegalArgumentException("scheduler must be null when bias is SYNC_BIAS");
        } else if (bias == Bias.ASYNC_BIAS && scheduler == null) {
            throw new IllegalArgumentException(
                    "must specify a non-null scheduler when bias is ASYNC_BIAS");
        }
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final AtomicReference<Drainer<T>> drainerRef = new AtomicReference<Drainer<T>>();
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(drainerRef);
        Queue<T> queue = new SpscArrayQueue<T>(capacity);
        Producer requestFromUpstream = new Producer() {
            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        };
        final Drainer<T> drainer;
        if (bias == Bias.SYNC_BIAS)
            drainer = DrainerSyncBiased.create(queue, child, requestFromUpstream);
        else
            drainer = DrainerAsyncBiased.create(new ConcurrentLinkedQueue<Object>(), child, scheduler.createWorker(),
                    child, requestFromUpstream);
        drainerRef.set(drainer);
        child.add(parent);
        child.setProducer(new Producer() {
            @Override
            public void request(long n) {
                //TODO if n == Long.MAX_VALUE
                drainer.request(n);
            }
        });
        return parent;
    }

    private static class ParentSubscriber<T> extends Subscriber<T> {

        private final AtomicReference<Drainer<T>> drainerRef;

        ParentSubscriber(final AtomicReference<Drainer<T>> drainerRef) {
            this.drainerRef = drainerRef;
        }

        void requestMore(long n) {
            if (n > 0)
                request(n);
        }

        @Override
        public void onCompleted() {
            drainerRef.get().onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            drainerRef.get().onError(e);
        }

        @Override
        public void onNext(T t) {
            drainerRef.get().onNext(t);
        }

    };

}
