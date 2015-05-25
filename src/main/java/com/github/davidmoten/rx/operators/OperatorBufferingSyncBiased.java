package com.github.davidmoten.rx.operators;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.internal.util.unsafe.SpscArrayQueue;

import com.github.davidmoten.util.DrainerSyncBiased;

public class OperatorBufferingSyncBiased<T> implements Operator<T, T> {

    private final int capacity;

    public OperatorBufferingSyncBiased(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final AtomicReference<DrainerSyncBiased<T>> drainerRef = new AtomicReference<DrainerSyncBiased<T>>();
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(drainerRef);
        Queue<T> queue = new SpscArrayQueue<T>(capacity);
        Producer requestFromUpstream = new Producer() {
            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        };
        final DrainerSyncBiased<T> drainer = new DrainerSyncBiased<T>(queue, child,
                requestFromUpstream);
        drainerRef.set(drainer);
        child.add(parent);
        child.setProducer(new Producer() {
            @Override
            public void request(long n) {
                drainer.request(n);
            }
        });
        return parent;
    }

    private static class ParentSubscriber<T> extends Subscriber<T> {

        private final AtomicReference<DrainerSyncBiased<T>> drainerRef;

        ParentSubscriber(final AtomicReference<DrainerSyncBiased<T>> drainerRef) {
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
