package com.github.davidmoten.rx.operators;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.Operator;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;

import com.github.davidmoten.util.Drainer;
import com.github.davidmoten.util.DrainerAsyncBiased;
import com.github.davidmoten.util.DrainerSyncBiased;

public class OperatorBufferEmissions<T> implements Operator<T, T> {

    private final Scheduler observeOnScheduler;

    public OperatorBufferEmissions() {
        this(null);
    }

    public OperatorBufferEmissions(Scheduler observeOnScheduler) {
        this.observeOnScheduler = observeOnScheduler;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final AtomicReference<Drainer<T>> drainerRef = new AtomicReference<Drainer<T>>();
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(drainerRef);
        Producer requestFromUpstream = new Producer() {
            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        };
        final Drainer<T> drainer;
        if (observeOnScheduler == null)
            drainer = DrainerSyncBiased.create(new ConcurrentLinkedQueue<T>(), child,
                    requestFromUpstream);
        else
            drainer = DrainerAsyncBiased.create(new ConcurrentLinkedQueue<Object>(), child,
                    observeOnScheduler.createWorker(), child, requestFromUpstream);
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
