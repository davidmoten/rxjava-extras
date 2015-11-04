package com.github.davidmoten.rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func0;
import rx.functions.Func2;

public final class OperatorCollectWhile<R, T> implements Operator<R, T> {

    private final Func0<? extends R> factory;
    private final Func2<? super R, ? super T, ? extends R> collector;
    private final Func2<? super R, ? super T, Boolean> condition;

    public OperatorCollectWhile(Func0<? extends R> factory,
            Func2<? super R, ? super T, ? extends R> collector,
            Func2<? super R, ? super T, Boolean> condition) {
        this.factory = factory;
        this.collector = collector;
        this.condition = condition;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> child) {

        return new Subscriber<T>(child) {

            R collection;

            @Override
            public void onCompleted() {
                if (collection != null) {
                    R c = collection;
                    collection = null;
                    child.onNext(c);
                    if (!child.isUnsubscribed())
                        child.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                collection = null;
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                try {
                    if (collection == null) {
                        collection = factory.call();
                    }
                    if (condition.call(collection, t)) {
                        collection = collector.call(collection, t);
                        request(1);
                    } else {
                        R c = collection;
                        collection = null;
                        if (!child.isUnsubscribed()) {
                            child.onNext(c);
                        }
                    }
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    onError(OnErrorThrowable.addValueAsLastCause(e, t));
                }
            }

        };
    }

}
