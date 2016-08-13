package com.github.davidmoten.rx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.github.davidmoten.rx.internal.operators.OnSubscribeFromQueue;
import com.github.davidmoten.rx.internal.operators.OrderedMerge;
import com.github.davidmoten.rx.internal.operators.Permutations;
import com.github.davidmoten.rx.internal.operators.Permutations.Swap;
import com.github.davidmoten.rx.observables.CachedObservable;
import com.github.davidmoten.util.Optional;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

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
     * reset). The interval is defined by {@code duration} and {@code unit} .
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
     * @param <T>
     *            the generic type of the source
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
     * except that the cache may be reset by the user calling
     * {@link CloseableObservableWithReset#reset}.
     * 
     * @param source
     *            the source observable
     * @param duration
     *            duration till next reset
     * @param unit
     *            units corresponding to the duration
     * @param scheduler
     *            scheduler to use for scheduling reset.
     * @param <T>
     *            generic type of source observable
     * @return {@link CloseableObservableWithReset} that should be closed once
     *         finished to prevent worker memory leak.
     */
    public static <T> CloseableObservableWithReset<T> cache(final Observable<T> source,
            final long duration, final TimeUnit unit, final Scheduler scheduler) {
        final AtomicReference<CachedObservable<T>> cacheRef = new AtomicReference<CachedObservable<T>>();
        final AtomicReference<Optional<Worker>> workerRef = new AtomicReference<Optional<Worker>>(
                Optional.<Worker> absent());
        CachedObservable<T> cache = new CachedObservable<T>(source);
        cacheRef.set(cache);
        Action0 closeAction = new Action0() {
            @Override
            public void call() {
                while (true) {
                    Optional<Worker> w = workerRef.get();
                    if (w == null) {
                        // we are finished
                        break;
                    } else {
                        if (workerRef.compareAndSet(w, null)) {
                            if (w.isPresent()) {
                                w.get().unsubscribe();
                            }
                            // we are finished
                            workerRef.set(null);
                            break;
                        }
                    }
                    // if not finished then try again
                }
            }
        };
        Action0 resetAction = new Action0() {

            @Override
            public void call() {
                startScheduledResetAgain(duration, unit, scheduler, cacheRef, workerRef);
            }
        };
        return new CloseableObservableWithReset<T>(cache, closeAction, resetAction);
    }

    private static <T> void startScheduledResetAgain(final long duration, final TimeUnit unit,
            final Scheduler scheduler, final AtomicReference<CachedObservable<T>> cacheRef,
            final AtomicReference<Optional<Worker>> workerRef) {

        Action0 action = new Action0() {
            @Override
            public void call() {
                cacheRef.get().reset();
            }
        };
        // CAS loop to cancel the current worker and create a new one
        while (true) {
            Optional<Worker> wOld = workerRef.get();
            if (wOld == null) {
                // we are finished
                return;
            }
            Optional<Worker> w = Optional.of(scheduler.createWorker());
            if (workerRef.compareAndSet(wOld, w)) {
                if (wOld.isPresent())
                    wOld.get().unsubscribe();
                w.get().schedule(action, duration, unit);
                break;
            }
        }
    }

    /**
     * Returns an Observable that epeats emitting {@code t} without completing.
     * Supports backpressure.
     * 
     * @param t value to repeat
     * @param <T> type of t

     * @return an observable that repeats t forever (or until unsubscribed)
     */
    public static <T> Observable<T> repeating(final T t) {
        return Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> subscriber) {
                subscriber.setProducer(new Producer() {
                    @Override
                    public void request(long n) {
                        while (n-- > 0 && !subscriber.isUnsubscribed()) {
                            subscriber.onNext(t);
                        }
                    }
                });
            }
        });
    }

    public static <T extends Comparable<? super T>> Observable<T> create(
            Collection<Observable<T>> sources) {
        return create(sources, false);
    }

    public static <T> Observable<T> create(Collection<Observable<T>> sources,
            Comparator<? super T> comparator) {
        return create(sources, comparator, false);
    }

    public static <T extends Comparable<? super T>> Observable<T> create(
            Collection<Observable<T>> sources, boolean delayErrors) {
        return OrderedMerge.create(sources, delayErrors);
    }

    public static <T> Observable<T> create(Collection<Observable<T>> sources,
            Comparator<? super T> comparator, boolean delayErrors) {
        return OrderedMerge.create(sources, comparator, delayErrors);
    }

    public static <T> Observable<T> fromQueue(Queue<T> queue) {
        return Observable.create(new OnSubscribeFromQueue<T>(queue));
    }

    public static <T> Observable<List<Integer>> permutations(int size) {
        List<Integer> indexes = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++) {
            indexes.add(i);
        }
        return Observable.from(Permutations.iterable(indexes)).scan(indexes,
                new Func2<List<Integer>, Swap<Integer>, List<Integer>>() {

                    @Override
                    public List<Integer> call(List<Integer> a, Swap<Integer> swap) {
                        List<Integer> b = new ArrayList<Integer>(a);
                        b.set(swap.left(), a.get(swap.right()));
                        b.set(swap.right(), a.get(swap.left()));
                        return b;
                    }
                });
    }

    public static <T> Observable<List<T>> permutations(final List<T> list) {
        return permutations(list.size()).map(new Func1<List<Integer>, List<T>>() {

            @Override
            public List<T> call(List<Integer> a) {
                List<T> b = new ArrayList<T>(a.size());
                for (int i = 0; i < a.size(); i++) {
                    b.add(list.get(a.get(i)));
                }
                return b;
            }
        });
    }
    
    
    public static Observable<Long> intervalLong(final long duration, final TimeUnit unit, final Scheduler scheduler) {
        return Observable.defer(new Func0<Observable<Long>>() {
            long[] count = new long[1];
            @Override
            public Observable<Long> call() {
                return Observable.interval(duration, unit, scheduler)
                        .map(new Func1<Long, Long>() {

                            @Override
                            public Long call(Long t) {
                                return count[0]++;
                            }});
            }});
    }
    
    public static Observable<Long> intervalLong(long duration, TimeUnit unit) {
        return intervalLong(duration, unit, Schedulers.computation());
    }

}