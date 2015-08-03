package com.github.davidmoten.rx.observables;

import rx.Observable;

import com.github.davidmoten.rx.operators.OnSubscribeCacheResetable;

public class CachedObservable<T> extends Observable<T> {

    private final OnSubscribeCacheResetable<T> cache;

    public CachedObservable(Observable<T> source) {
        this(new OnSubscribeCacheResetable<T>(source));
    }

    CachedObservable(OnSubscribeCacheResetable<T> cache) {
        super(cache);
        this.cache = cache;
    }

    public CachedObservable<T> reset() {
        cache.reset();
        return this;
    }

}
