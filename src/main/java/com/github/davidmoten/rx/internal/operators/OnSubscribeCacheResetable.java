package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

public final class OnSubscribeCacheResetable<T> implements OnSubscribe<T> {

    private final AtomicBoolean refresh = new AtomicBoolean(true);
    private final Observable<T> source;
    private volatile Observable<T> current;

    public OnSubscribeCacheResetable(Observable<T> source) {
        this.source = source;
        this.current = source;
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        if (refresh.compareAndSet(true, false)) {
            current = source.cache();
        }
        current.unsafeSubscribe(subscriber);
    }

    public void reset() {
        refresh.set(true);
    }

}
