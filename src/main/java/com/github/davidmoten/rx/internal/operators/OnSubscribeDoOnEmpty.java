package com.github.davidmoten.rx.internal.operators;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;

public final class OnSubscribeDoOnEmpty<T> implements OnSubscribe<T> {

    private final Action0 onEmpty;
    private Observable<T> observable;

    public OnSubscribeDoOnEmpty(Observable<T> observable, Action0 onEmpty) {
        this.observable = observable;
        this.onEmpty = onEmpty;
    }

    @Override
    public void call(final Subscriber<? super T> child) {
        Subscriber<T> subscriber = createSubscriber(child, onEmpty);
        observable.unsafeSubscribe(subscriber);
    }

    private static <T> Subscriber<T> createSubscriber(final Subscriber<? super T> child, final Action0 onEmpty) {
        return new Subscriber<T>(child) {

            private boolean isEmpty = true;
            private boolean done = false;

            @Override
            public void onCompleted() {
                if (done) {
                    return;
                }
                if (isEmpty) {
                    try {
                        onEmpty.call();
                    } catch (Throwable e) {
                        Exceptions.throwOrReport(e,this);
                        return;
                    }
                    if (!isUnsubscribed()) {
                        child.onCompleted();
                    }
                } else {
                    child.onCompleted();
                }
                done = true;
            }

            @Override
            public void onError(Throwable e) {
                if (done) {
                    return;
                }
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                if (done) {
                    return;
                }
                isEmpty = false;
                child.onNext(t);
            }
        };
    }

}