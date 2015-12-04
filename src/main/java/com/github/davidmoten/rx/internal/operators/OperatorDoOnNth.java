package com.github.davidmoten.rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Action1;

public final class OperatorDoOnNth<T> implements Operator<T, T> {

    public static <T> OperatorDoOnNth<T> create(Action1<? super T> action, int n) {
        return new OperatorDoOnNth<T>(action, n);
    }

    private final Action1<? super T> action;
    private final int n;

    private OperatorDoOnNth(Action1<? super T> action, int n) {
        this.action = action;
        this.n = n;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {
            int count;

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
                count++;
                if (count == n) {
                    action.call(t);
                }
                child.onNext(t);
            }

        };
    }

}
