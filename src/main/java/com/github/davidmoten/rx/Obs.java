package com.github.davidmoten.rx;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.github.davidmoten.rx.observables.CachedObservable;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public final class Obs {

    /**
     * Returns a cached {@link Observable} like {@link Observable#cache()}
     * except that the cache can be reset by calling
     * {@link CachedObservable#reset()}.
     * 
     * @param source
     *            the observable to be cached.
     * @param <T>
     *            the generic type of the source
     * @return a cached observable whose cache can be reset.
     */
    public static <T> CachedObservable<T> cache(Observable<T> source) {
        return new CachedObservable<T>(source);
    }

    /**
     * Returns a cached {@link Observable} like {@link Observable#cache()}
     * except that the cache can be reset by calling
     * {@link CachedObservable#reset()} and the cache will be automatically
     * reset an interval after first subscription (or first subscription after
     * reset). The interval is defined by {@link code duration} and {@code unit}
     * .
     * 
     * @param source
     *            the source observable
     * @param duration
     *            duration till next reset
     * @param unit
     *            units corresponding to the duration
     * @param scheduler
     *            scheduler to use for scheduling reset
     * @return cached observable that resets regularly on a time interval
     */
    public static <T> Observable<T> cache(Observable<T> source, long duration, TimeUnit unit,
            Scheduler scheduler) {
        final AtomicReference<CachedObservable<T>> cacheRef = new AtomicReference<CachedObservable<T>>();
        @SuppressWarnings("unchecked")
        Observable<T> mergeWithTimer = (Observable<T>) Observable.timer(duration, unit, scheduler)
                .doOnNext(new Action1<Long>() {
                    @Override
                    public void call(Long n) {
                        cacheRef.get().reset();
                    }
                }).ignoreElements().cast(Object.class).mergeWith(source.cast(Object.class));
        CachedObservable<T> cache = new CachedObservable<T>(mergeWithTimer);
        cacheRef.set(cache);
        return cache;
    }

    /**
     * Returns a cached {@link Observable} like {@link Observable#cache()}
     * except that the cache can be reset by calling
     * {@link CachedObservable#reset()} and the cache will be automatically
     * reset an interval after first subscription (or first subscription after
     * reset). The interval is defined by {@link code duration} and {@code unit}
     * . Cache {@code reset}s are scheduled on {@link Schedulers.computation}.
     * 
     * @param source
     *            the source observable
     * @param duration
     *            duration till next reset
     * @param unit
     *            units corresponding to the duration
     * @return cached observable that resets regularly on a time interval
     */
    public static <T> Observable<T> cache(Observable<T> source, long duration, TimeUnit unit) {
        return cache(source, duration, unit, Schedulers.computation());
    }

}
