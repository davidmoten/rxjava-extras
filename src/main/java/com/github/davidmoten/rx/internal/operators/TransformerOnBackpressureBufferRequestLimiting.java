package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicLong;

import com.github.davidmoten.rx.Actions;
import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.util.BackpressureUtils;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

public final class TransformerOnBackpressureBufferRequestLimiting<T> implements Transformer<T, T> {

    private static final TransformerOnBackpressureBufferRequestLimiting<Object> instance = new TransformerOnBackpressureBufferRequestLimiting<Object>();

    @SuppressWarnings("unchecked")
    public static final <T> TransformerOnBackpressureBufferRequestLimiting<T> instance() {
        return (TransformerOnBackpressureBufferRequestLimiting<T>) instance;
    }

    @Override
    public Observable<T> call(final Observable<T> o) {
        return Observable.defer(new Func0<Observable<T>>() {
            @Override
            public Observable<T> call() {
                final OperatorPassThroughAdjustedRequest<T> op = new OperatorPassThroughAdjustedRequest<T>();
                return o.lift(op).onBackpressureBuffer().doOnRequest(new Action1<Long>() {

                    @Override
                    public void call(Long n) {
                        op.requestMore(n);
                    }
                });
            }
        });
    }

    /**
     * Only used with an immediate downstream operator that requests
     * {@code Long.MAX_VALUE} and should only be subscribed to once (use it
     * within a {@code defer} block}.
     *
     * @param <T>
     *            stream item type
     */
    private static final class OperatorPassThroughAdjustedRequest<T> implements Operator<T, T> {

        private volatile ParentSubscriber<T> parent;
        private final AtomicLong requested = new AtomicLong();
        private final Object lock = new Object();

        @Override
        public Subscriber<? super T> call(Subscriber<? super T> child) {
            // this method should only be called once for this instance
            // assume child requests MAX_VALUE
            ParentSubscriber<T> p = new ParentSubscriber<T>(child);
            synchronized (lock) {
                parent = p;
            }
            p.requestMore(requested.get());
            child.add(p);
            return p;
        }

        public void requestMore(long n) {
            ParentSubscriber<T> p = parent;
            if (p != null) {
                p.requestMore(n);
            } else {
                synchronized (lock) {
                    ParentSubscriber<T> par = parent;
                    if (par == null) {
                        BackpressureUtils.getAndAddRequest(requested, n);
                    } else {
                        par.requestMore(n);
                    }
                }
            }
        }

    }

    private static final class ParentSubscriber<T> extends Subscriber<T> {

        private final Subscriber<? super T> child;
        private final AtomicLong expected = new AtomicLong();

        public ParentSubscriber(Subscriber<? super T> child) {
            this.child = child;
            request(0);
        }

        public void requestMore(long n) {
            if (n <= 0) {
                return;
            }
            long r = expected.get();
            if (r == Long.MAX_VALUE) {
                return;
            } else {
                long u = r;
                while (true) {
                    long sum = u + n;
                    final long v;
                    if (sum < 0) {
                        v = Long.MAX_VALUE;
                    } else {
                        v = sum;
                    }
                    if (expected.compareAndSet(u, v)) {
                        // if v negative (more have arrived than requested)
                        long diff = Math.max(0, v);
                        long req = Math.min(n, diff);
                        if (req > 0) {
                            request(req);
                        }
                        return;
                    } else {
                        u = expected.get();
                    }
                }
            }
        }

        @Override
        public void onCompleted() {
            child.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            expected.decrementAndGet();
            child.onNext(t);
        }

    }

    public static void main(String[] args) throws InterruptedException {
        Observable.range(1, 10000) //
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long n) {
                        System.out.println("requested " + n);
                    }
                }).doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        System.out.println("unsubscribed");
                    }
                }) //
                .compose(Transformers.<Integer> onBackpressureBufferRequestLimiting()) //
                .take(10) //
                .subscribeOn(Schedulers.io()) //
                .doOnNext(Actions.println()) //
                .count().toBlocking().single();
        Thread.sleep(2000);
    }

}
