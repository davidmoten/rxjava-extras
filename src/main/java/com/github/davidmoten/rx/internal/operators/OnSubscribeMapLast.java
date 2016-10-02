package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorThrowable;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Func1;

public final class OnSubscribeMapLast<T> implements OnSubscribe<T> {

    private final Observable<T> source;
    private final Func1<? super T, ? extends T> function;

    public OnSubscribeMapLast(Observable<T> source, Func1<? super T, ? extends T> function) {
        this.source = source;
        this.function = function;
    }

    @Override
    public void call(Subscriber<? super T> child) {
        final MapLastSubscriber<T> parent = new MapLastSubscriber<T>(child, function);
        child.add(parent);
        child.setProducer(new Producer() {
            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        });
        source.unsafeSubscribe(parent);
    }

    private final static class MapLastSubscriber<T> extends Subscriber<T> {

        private static final Object EMPTY = new Object();

        private final Subscriber<? super T> child;
        private final Func1<? super T, ? extends T> function;
        private final AtomicBoolean firstRequest = new AtomicBoolean(true);
        @SuppressWarnings("unchecked")
        private T value = (T) EMPTY;

        public MapLastSubscriber(Subscriber<? super T> child,
                Func1<? super T, ? extends T> function) {
            this.child = child;
            this.function = function;
        }

        public void requestMore(long n) {
            if (n < 0) {
                throw new IllegalArgumentException("cannot request negative amount");
            } else if (n == 0) {
                return;
            } else if (firstRequest.compareAndSet(true, false)) {
                long m = n + 1;
                if (m < 0) {
                    m = Long.MAX_VALUE;
                }
                request(m);
            } else {
                request(n);
            }
        }

        @Override
        public void onCompleted() {
            if (value != EMPTY) {
                T value2;
                try {
                    value2 = function.call(value);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    onError(OnErrorThrowable.addValueAsLastCause(e, value));
                    return;
                }
                child.onNext(value2);
            }
            child.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            if (value != EMPTY) {
                child.onNext(value);
            }
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            if (value == EMPTY) {
                value = t;
            } else {
                child.onNext(value);
                value = t;
            }
        }

    }

}
