package com.github.davidmoten.rx.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Action1;

public final class OperatorDoAfterOnNext<T> implements Operator<T,T> {

    //micro-optimisation drop private modifier
    final Action1<T> action;

    public OperatorDoAfterOnNext(Action1<T> action) {
        this.action = action;
    }
    
    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {

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
                child.onNext(t);
                action.call(t);
            }
        };
    }

    
}
