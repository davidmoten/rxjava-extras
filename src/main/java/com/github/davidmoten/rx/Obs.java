package com.github.davidmoten.rx;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.github.davidmoten.rx.observables.CachedObservable;

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.functions.Action0;
import rx.functions.Action1;

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
     * @param worker
     *            worker to use for scheduling reset. Don't forget to
     *            unsubscribe the worker when no longer required.
     * @return cached observable that resets regularly on a time interval
     */
    public static <T> Observable<T> cache(final Observable<T> source, final long duration,
            final TimeUnit unit, final Worker worker) {
        final AtomicReference<CachedObservable<T>> cacheRef = new AtomicReference<CachedObservable<T>>();

        CachedObservable<T> cache = new CachedObservable<T>(source);
        cacheRef.set(cache);
        return cache.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                Action0 action = new Action0() {

                    @Override
                    public void call() {
                        cacheRef.get().reset();
                    }
                };
                worker.schedule(action, duration, unit);
            }
        });
    }

    /**
     * Returns a cached {@link Observable} like {@link Observable#cache()}
     * except that the cache will be automatically reset after the given
     * interval using the given scheduler according to the criteria given by
     * {@code rescheduleOnSubscribe}, {@code rescheduleOnNext},
     * {@code rescheduleOnError}. .
     * 
     * @param source
     *            the source observable
     * @param duration
     *            duration till next reset
     * @param unit
     *            units corresponding to the duration
     * @param scheduler
     *            scheduler to use for scheduling reset.
     * @param rescheduleOnSubscribe
     *            set to true if a new subscription should restart the scheduled
     *            cache reset
     * @param rescheduleOnNext
     *            set to true if an onNext emission should restart the scheduled
     *            cache reset
     * @param rescheduleOnError
     *            set to true if an error should restart the scheduled cache
     *            reset
     * @param <T>
     *            generic type of source observable
     * @return {@link CloseableObservable} that should be closed once finished
     *         to prevent worker memory leak
     */
    public static <T> CloseableObservable<T> cache(final Observable<T> source, final long duration,
            final TimeUnit unit, final Scheduler scheduler, final boolean rescheduleOnSubscribe,
            final boolean rescheduleOnNext, boolean rescheduleOnError) {
        final AtomicReference<CachedObservable<T>> cacheRef = new AtomicReference<CachedObservable<T>>();
        final AtomicReference<Worker> workerRef = new AtomicReference<Worker>();
        CachedObservable<T> cache = new CachedObservable<T>(source);
        cacheRef.set(cache);
        Observable<T> o = cache.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                if (rescheduleOnSubscribe)
                    startScheduledResetAgain(duration, unit, scheduler, cacheRef, workerRef);
            }
        }).doOnNext(new Action1<T>() {
            @Override
            public void call(T t) {
                if (rescheduleOnNext)
                    startScheduledResetAgain(duration, unit, scheduler, cacheRef, workerRef);
            }
        }).doOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                if (rescheduleOnNext)
                    startScheduledResetAgain(duration, unit, scheduler, cacheRef, workerRef);
            }
        });
        Action0 closeAction = new Action0() {
            @Override
            public void call() {
                if (workerRef.get() != null)
                    workerRef.get().unsubscribe();
            }
        };
        return new CloseableObservable<T>(o, closeAction);
    }

    private static <T> void startScheduledResetAgain(final long duration, final TimeUnit unit,
            final Scheduler scheduler, final AtomicReference<CachedObservable<T>> cacheRef,
            final AtomicReference<Worker> workerRef) {
        while (true) {
            Worker wOld = workerRef.get();
            Worker w = scheduler.createWorker();
            if (workerRef.compareAndSet(wOld, w)) {
                if (wOld != null)
                    wOld.unsubscribe();
                break;
            }
        }
        Action0 action = new Action0() {
            @Override
            public void call() {
                cacheRef.get().reset();
            }
        };
        workerRef.get().schedule(action, duration, unit);
    }

}
