package com.github.davidmoten.rx;

import static rx.Observable.just;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.rx.internal.operators.OperatorBufferEmissions;
import com.github.davidmoten.rx.internal.operators.OperatorDoOnNth;
import com.github.davidmoten.rx.internal.operators.OperatorFromTransformer;
import com.github.davidmoten.rx.internal.operators.OperatorOrderedMerge;
import com.github.davidmoten.rx.internal.operators.TransformerStateMachine;
import com.github.davidmoten.rx.util.MapWithIndex;
import com.github.davidmoten.rx.util.MapWithIndex.Indexed;
import com.github.davidmoten.rx.util.Pair;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.schedulers.Schedulers;

public final class Transformers {

    public static <T, R> Operator<R, T> toOperator(
            Func1<? super Observable<T>, ? extends Observable<R>> function) {
        return OperatorFromTransformer.toOperator(function);
    }

    public static <T extends Number> Transformer<T, Statistics> collectStats() {
        return new Transformer<T, Statistics>() {

            @Override
            public Observable<Statistics> call(Observable<T> o) {
                return o.scan(Statistics.create(), Functions.collectStats());
            }
        };
    }

    public static <T, R extends Number> Transformer<T, Pair<T, Statistics>> collectStats(
            final Func1<? super T, ? extends R> function) {
        return new Transformer<T, Pair<T, Statistics>>() {

            @Override
            public Observable<Pair<T, Statistics>> call(Observable<T> source) {
                return source.scan(Pair.create((T) null, Statistics.create()),
                        new Func2<Pair<T, Statistics>, T, Pair<T, Statistics>>() {
                    @Override
                    public Pair<T, Statistics> call(Pair<T, Statistics> pair, T t) {
                        return Pair.create(t, pair.b().add(function.call(t)));
                    }
                }).skip(1);
            }
        };
    }

    public static <T extends Comparable<? super T>> Transformer<T, T> sort() {
        return new Transformer<T, T>() {

            @Override
            public Observable<T> call(Observable<T> o) {
                return o.toSortedList().flatMapIterable(Functions.<List<T>> identity());
            }
        };
    }

    public static <T> Transformer<T, T> sort(final Comparator<? super T> comparator) {
        return new Transformer<T, T>() {

            @Override
            public Observable<T> call(Observable<T> o) {
                return o.toSortedList(Functions.toFunc2(comparator))
                        .flatMapIterable(Functions.<List<T>> identity());
            }
        };
    }

    public static <T> Transformer<T, Set<T>> toSet() {
        return new Transformer<T, Set<T>>() {

            @Override
            public Observable<Set<T>> call(Observable<T> o) {
                return o.collect(new Func0<Set<T>>() {

                    @Override
                    public Set<T> call() {
                        return new HashSet<T>();
                    }
                }, new Action2<Set<T>, T>() {

                    @Override
                    public void call(Set<T> set, T t) {
                        set.add(t);
                    }
                });
            }
        };
    }

    public static <T> Transformer<T, Indexed<T>> mapWithIndex() {
        return MapWithIndex.instance();
    }

    /**
     * Returns a {@link Transformer} that allows processing of the source stream
     * to be defined in a state machine where transitions of the state machine
     * may also emit items to downstream that are buffered if necessary when
     * backpressure is requested. <code>flatMap</code> is part of the processing
     * chain so the source may experience requests for more items than are
     * strictly required by the endpoint subscriber.
     * 
     * <p>
     * Internally this transformer uses {@link Observable#scan} emitting a
     * stream of new states composed with emissions from the transition to each
     * state and {@link Observable#flatMap} to emit the recorded emissions with
     * backpressure.
     * 
     * @param initialStateFactory
     *            the factory to create the initial state of the state machine.
     * @param transition
     *            defines state transitions and consequent emissions to
     *            downstream when an item arrives from upstream
     * @param completionAction
     *            defines activity that should happen based on the final state
     *            just before downstream <code>onCompleted()</code> is called.
     *            For example any buffered emissions in state could be emitted
     *            at this point. Don't call <code>observer.onCompleted()</code>
     *            as it is called for you after the action completes.
     * @param <State>
     *            the class representing the state of the state machine
     * @param <In>
     *            the input observable type
     * @param <Out>
     *            the output observable type
     * @throws NullPointerException
     *             if {@code initialStateFactory} or {@code transition},or
     *             {@code completionAction} is null
     * @return a backpressure supporting transformer that implements the state
     *         machine specified by the parameters
     */
    public static <State, In, Out> Transformer<In, Out> stateMachine(
            Func0<State> initialStateFactory,
            Func3<? super State, ? super In, ? super Observer<Out>, ? extends State> transition,
            Action2<? super State, ? super Observer<Out>> completionAction) {
        return TransformerStateMachine.<State, In, Out> create(initialStateFactory, transition,
                completionAction);
    }

    /**
     * Returns a {@link Transformer} that allows processing of the source stream
     * to be defined in a state machine where transitions of the state machine
     * may also emit items to downstream that are buffered if necessary when
     * backpressure is requested. <code>flatMap</code> is part of the processing
     * chain so the source may experience requests for more items than are
     * strictly required by the endpoint subscriber.
     * 
     * <p>
     * Internally this transformer uses {@link Observable#scan} emitting a
     * stream of new states composed with emissions from the transition to each
     * state and {@link Observable#flatMap} to emit the recorded emissions with
     * backpressure.
     * 
     * @param initialState
     *            the initial state of the state machine.
     * @param transition
     *            defines state transitions and consequent emissions to
     *            downstream when an item arrives from upstream
     * @param completionAction
     *            defines activity that should happen based on the final state
     *            just before downstream <code>onCompleted()</code> is called.
     *            For example any buffered emissions in state could be emitted
     *            at this point. Don't call <code>observer.onCompleted()</code>
     *            as it is called for you after the action completes.
     * @param <State>
     *            the class representing the state of the state machine
     * @param <In>
     *            the input observable type
     * @param <Out>
     *            the output observable type
     * @throws NullPointerException
     *             if {@code transition} or {@code completionAction} is null
     * @return a backpressure supporting transformer that implements the state
     *         machine specified by the parameters
     */
    public static <State, In, Out> Transformer<In, Out> stateMachine(State initialState,
            Func3<? super State, ? super In, ? super Observer<Out>, ? extends State> transition,
            Action2<? super State, ? super Observer<Out>> completionAction) {
        Func0<State> f = Functions.constant0(initialState);
        return TransformerStateMachine.<State, In, Out> create(f, transition, completionAction);
    }

    /**
     * Returns a {@link Transformer} that allows processing of the source stream
     * to be defined in a state machine where transitions of the state machine
     * may also emit items to downstream that are buffered if necessary when
     * backpressure is requested. <code>flatMap</code> is part of the processing
     * chain so the source may experience requests for more items than are
     * strictly required by the endpoint subscriber. This overload uses a do
     * nothing {@code completionAction} which may leave some emissions recorded
     * in State as unemitted.
     * 
     * <p>
     * Internally this transformer uses {@link Observable#scan} emitting a
     * stream of new states composed with emissions from the transition to each
     * state and {@link Observable#flatMap} to emit the recorded emissions with
     * backpressure.
     * 
     * @param initialState
     *            the initial state of the state machine.
     * @param transition
     *            defines state transitions and consequent emissions to
     *            downstream when an item arrives from upstream
     * @param <State>
     *            the class representing the state of the state machine
     * @param <In>
     *            the input observable type
     * @param <Out>
     *            the output observable type
     * @throws NullPointerException
     *             if {@code initialState} or {@code transition} is null
     * @return a backpressure supporting transformer that implements the state
     *         machine specified by the parameters
     */
    public static <State, In, Out> Transformer<In, Out> stateMachine(State initialState,
            Func3<? super State, ? super In, ? super Observer<Out>, ? extends State> transition) {
        Func0<State> f = Functions.constant0(initialState);
        return TransformerStateMachine.<State, In, Out> create(f, transition,
                new Action2<State, Observer<Out>>() {
                    @Override
                    public void call(State state, Observer<Out> observer) {
                        // do nothing
                    }
                });
    }

    @SuppressWarnings("unchecked")
    public static <T> Transformer<T, T> bufferEmissions() {
        return (Transformer<T, T>) BufferEmissionsHolder.INSTANCE;
    }

    // holder lazy singleton pattern
    private static class BufferEmissionsHolder {
        static Transformer<Object, Object> INSTANCE = new Transformer<Object, Object>() {

            @Override
            public Observable<Object> call(Observable<Object> o) {
                return o.lift(new OperatorBufferEmissions<Object>());
            }
        };
    }

    /**
     * Returns the source {@link Observable} merged with the <code>other</code>
     * observable using the given {@link Comparator} for order. A precondition
     * is that the source and other are already ordered. This transformer does
     * not support backpressure but its inputs must support backpressure. If you
     * need backpressure support then compose with
     * <code>.onBackpressureXXX</code>.
     * 
     * @param other
     *            the other already ordered observable
     * @param comparator
     *            the ordering to use
     * @param <T>
     *            the generic type of the objects being compared
     * @return merged and ordered observable
     */
    public static final <T> Transformer<T, T> orderedMergeWith(final Observable<T> other,
            final Func2<? super T, ? super T, Integer> comparator) {
        return new Transformer<T, T>() {

            @Override
            public Observable<T> call(Observable<T> source) {
                return source.lift(new OperatorOrderedMerge<T>(other, comparator));
            }
        };
    }

    /**
     * Returns a {@link Transformer} that returns an {@link Observable} that is
     * a buffering of the source Observable into lists of sequential items that
     * are equal.
     * 
     * <p>
     * For example, the stream
     * {@code Observable.just(1, 1, 2, 2, 1).compose(toListUntilChanged())}
     * would emit {@code [1,1], [2], [1]}.
     * 
     * @param <T>
     *            the generic type of the source Observable
     * @return transformer as above
     */
    public static <T> Transformer<T, List<T>> toListUntilChanged() {
        Func2<Collection<T>, T, Boolean> equal = HolderEquals.instance();
        return toListWhile(equal);
    }

    private static class HolderEquals {
        private static final Func2<Collection<Object>, Object, Boolean> INSTANCE = new Func2<Collection<Object>, Object, Boolean>() {
            @Override
            public Boolean call(Collection<Object> list, Object t) {
                return list.isEmpty() || list.iterator().next().equals(t);
            }
        };

        @SuppressWarnings("unchecked")
        static <T> Func2<Collection<T>, T, Boolean> instance() {
            return (Func2<Collection<T>, T, Boolean>) (Func2<?, ?, Boolean>) INSTANCE;
        }
    }

    /**
     * Returns a {@link Transformer} that returns an {@link Observable} that is
     * a buffering of the source Observable into lists of sequential items that
     * satisfy the condition {@code condition}.
     * 
     * @param condition
     *            condition function that must return true if an item is to be
     *            part of the list being prepared for emission
     * @param <T>
     *            the generic type of the source Observable
     * @return transformer as above
     */
    public static <T> Transformer<T, List<T>> toListWhile(
            final Func2<? super List<T>, ? super T, Boolean> condition) {

        Func0<List<T>> initialState = new Func0<List<T>>() {
            @Override
            public List<T> call() {
                return new ArrayList<T>();
            }
        };

        Action2<List<T>, T> collect = new Action2<List<T>, T>() {

            @Override
            public void call(List<T> list, T n) {
                list.add(n);
            }
        };
        return collectWhile(initialState, collect, condition);
    }

    /**
     * Returns a {@link Transformer} that returns an {@link Observable} that is
     * collected into {@code Collection} instances created by {@code factory}
     * that are emitted when items are not equal or on completion.
     * 
     * @param factory
     *            collection instance creator
     * @param collect
     *            collection action
     * @param <T>
     *            generic type of source observable
     * @param <R>
     *            collection type emitted by transformed Observable
     * @return transformer as above
     */
    public static <T, R extends Collection<T>> Transformer<T, R> collectWhile(
            final Func0<R> factory, final Action2<? super R, ? super T> collect) {
        return collectWhile(factory, collect, HolderEquals.<T> instance());
    }

    /**
     * Returns a {@link Transformer} that returns an {@link Observable} that is
     * collected into {@code Collection} instances created by {@code factory}
     * that are emitted when the collection and latest emission do not satisfy
     * {@code condition} or on completion.
     * 
     * @param factory
     *            collection instance creator
     * @param collect
     *            collection action
     * @param condition
     *            returns true if and only if emission should be collected in
     *            current collection being prepared for emission
     * @param <T>
     *            generic type of source observable
     * @param <R>
     *            collection type emitted by transformed Observable
     * @return transformer as above
     */
    public static <T, R extends Collection<T>> Transformer<T, R> collectWhile(
            final Func0<R> factory, final Action2<? super R, ? super T> collect,
            final Func2<? super R, ? super T, Boolean> condition) {
        Func3<R, T, Observer<R>, R> transition = new Func3<R, T, Observer<R>, R>() {

            @Override
            public R call(R collection, T t, Observer<R> observer) {
                if (condition.call(collection, t)) {
                    collect.call(collection, t);
                    return collection;
                } else {
                    observer.onNext(collection);
                    R r = factory.call();
                    collect.call(r, t);
                    return r;
                }
            }

        };
        Action2<R, Observer<R>> completionAction = new Action2<R, Observer<R>>() {
            @Override
            public void call(R collection, Observer<R> observer) {
                if (!collection.isEmpty()) {
                    observer.onNext(collection);
                }
            }
        };
        return Transformers.stateMachine(factory, transition, completionAction);
    }

    public static <T> Transformer<T, T> retry(int numRetries, long wait, TimeUnit unit,
            Action1<? super ErrorAndWait> action, final Scheduler scheduler) {
        return retry(Observable.just(wait).repeat(numRetries), unit, action, scheduler);
    }

    public static <T> Transformer<T, T> retry(int numRetries, long wait, TimeUnit unit,
            Action1<? super ErrorAndWait> action) {
        return retry(numRetries, wait, unit, action, Schedulers.computation());
    }

    public static <T> Transformer<T, T> retry(int numRetries, long wait, TimeUnit unit,
            final Scheduler scheduler) {
        return retry(Observable.just(wait).repeat(numRetries), unit, Actions.doNothing1(),
                scheduler);
    }

    public static <T> Transformer<T, T> retry(int numRetries, long wait, TimeUnit unit) {
        return retry(numRetries, wait, unit, Schedulers.computation());
    }

    public static <T> Transformer<T, T> retry(final Observable<Long> waits, TimeUnit unit,
            final Action1<? super ErrorAndWait> action) {
        return retry(waits, unit, action, Schedulers.computation());
    }

    public static <T> Transformer<T, T> retry(final Observable<Long> waits, TimeUnit unit,
            final Action1<? super ErrorAndWait> action, final Scheduler scheduler) {
        final Action1<ErrorAndWait> action2 = new Action1<ErrorAndWait>() {

            @Override
            public void call(ErrorAndWait e) {
                if (e.waitMs() != -1)
                    action.call(e);
            }

        };
        return new Transformer<T, T>() {

            @Override
            public Observable<T> call(Observable<T> source) {
                Func1<Observable<? extends Throwable>, Observable<?>> notificationHandler = new Func1<Observable<? extends Throwable>, Observable<?>>() {

                    @Override
                    public Observable<ErrorAndWait> call(Observable<? extends Throwable> errors) {

                        return errors
                                // zip with waits
                                .zipWith(waits.concatWith(just(-1L)), ErrorAndWait.toErrorAndWait)
                                // perform user action (for example log that a
                                // wait is happening)
                                .doOnNext(action2)
                                // wait the time in ErrorAndWait
                                .flatMap(Transformers.wait(scheduler));
                    }
                };
                return source.retryWhen(notificationHandler);
            }
        };
    }

    public static <T> Transformer<T, T> retryExponentialBackoff(final int numRetries,
            final long firstWait, final TimeUnit unit, final Scheduler scheduler) {
        return retryExponentialBackoff(numRetries, firstWait, unit, DO_NOTHING, scheduler);
    }

    public static <T> Transformer<T, T> retryExponentialBackoff(final int numRetries,
            final long firstWait, final TimeUnit unit, final Action1<? super ErrorAndWait> action) {
        return retryExponentialBackoff(numRetries, firstWait, unit, action,
                Schedulers.computation());
    }

    public static <T> Transformer<T, T> retryExponentialBackoff(final int numRetries,
            final long firstWait, final TimeUnit unit, final Action1<? super ErrorAndWait> action,
            final Scheduler scheduler) {
        // create exponentially increasing waits
        final Observable<Long> waits = Observable.range(1, numRetries)
                // make exponential
                .map(new Func1<Integer, Long>() {
                    @Override
                    public Long call(Integer n) {
                        return (long) Math.pow(2, n - 1) * unit.toMillis(firstWait);
                    }
                });
        return retry(waits, unit, action, scheduler);
    }

    public static class ErrorAndWait {
        private final Throwable throwable;
        private final long waitMs;

        ErrorAndWait(Throwable throwable, long waitMs) {
            this.throwable = throwable;
            this.waitMs = waitMs;
        }

        public Throwable throwable() {
            return throwable;
        }

        public long waitMs() {
            return waitMs;
        }

        static Func2<Throwable, Long, ErrorAndWait> toErrorAndWait = new Func2<Throwable, Long, ErrorAndWait>() {
            @Override
            public ErrorAndWait call(Throwable throwable, Long waitMs) {
                return new ErrorAndWait(throwable, waitMs);
            }
        };
    }

    private static Func1<ErrorAndWait, Observable<ErrorAndWait>> wait(final Scheduler scheduler) {
        return new Func1<ErrorAndWait, Observable<ErrorAndWait>>() {
            @Override
            public Observable<ErrorAndWait> call(ErrorAndWait e) {
                if (e.waitMs == -1)
                    return Observable.error(e.throwable);
                else
                    return Observable.timer(e.waitMs, TimeUnit.MILLISECONDS, scheduler)
                            .map(Functions.constant(e));
            }
        };
    }

    public static <T> Transformer<T, T> doOnNext(final int n, final Action1<? super T> action) {
        return new Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> o) {
                return o.lift(OperatorDoOnNth.create(action, n));
            }
        };
    }

    public static <T> Transformer<T, T> doOnFirst(final Action1<? super T> action) {
        return doOnNext(1, action);
    }

}