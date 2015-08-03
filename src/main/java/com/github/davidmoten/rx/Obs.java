package com.github.davidmoten.rx;

import rx.Observable;

import com.github.davidmoten.rx.observables.CachedObservable;

public final class Obs {

    /**
     * Returns a cached {@link Observable} like {@link Observable#cache()}
     * except that the cache can be reset by calling
     * {@link CachedObservable#reset()}.
     * 
     * @param source
     *            the observable to be cached.
     * @return a cached observable whose cache can be reset.
     */
    public static <T> CachedObservable<T> cache(Observable<T> source) {
        return new CachedObservable<T>(source);
    }

}
