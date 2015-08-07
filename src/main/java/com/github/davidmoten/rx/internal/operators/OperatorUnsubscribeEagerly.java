package com.github.davidmoten.rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;

public final class OperatorUnsubscribeEagerly<T> implements Operator<T, T> {

    private OperatorUnsubscribeEagerly() {
        // no instantiation outside of this class
    }

    private static final class Singleton {
        private static final OperatorUnsubscribeEagerly<?> INSTANCE = new OperatorUnsubscribeEagerly<Object>();
    }

    @SuppressWarnings("unchecked")
    public static final <T> OperatorUnsubscribeEagerly<T> instance() {
        return (OperatorUnsubscribeEagerly<T>) Singleton.INSTANCE;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        Subscriber<T> parent = new Subscriber<T>() {

            @Override
            public void onCompleted() {
                unsubscribe();
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                unsubscribe();
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                child.onNext(t);
            }

        };
        child.add(parent);
        return parent;
    }

}
