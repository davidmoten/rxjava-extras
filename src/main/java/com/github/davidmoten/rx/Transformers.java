package com.github.davidmoten.rx;

import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.davidmoten.rx.internal.operators.OperatorBufferEmissions;
import com.github.davidmoten.rx.internal.operators.OperatorDoOnNth;
import com.github.davidmoten.rx.internal.operators.OperatorFromTransformer;
import com.github.davidmoten.rx.internal.operators.OperatorSampleFirst;
import com.github.davidmoten.rx.internal.operators.OrderedMerge;
import com.github.davidmoten.rx.internal.operators.TransformerDecode;
import com.github.davidmoten.rx.internal.operators.TransformerLimitSubscribers;
import com.github.davidmoten.rx.internal.operators.TransformerStateMachine;
import com.github.davidmoten.rx.internal.operators.TransformerStringSplit;
import com.github.davidmoten.rx.util.BackpressureStrategy;
import com.github.davidmoten.rx.util.MapWithIndex;
import com.github.davidmoten.rx.util.MapWithIndex.Indexed;
import com.github.davidmoten.rx.util.Pair;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
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
     * @param initialStateFactory
     *            the factory to create the initial state of the state machine.
     * @param transition
     *            defines state transitions and consequent emissions to
     *            downstream when an item arrives from upstream. The
     *            {@link Subscriber} is called with the emissions to downstream.
     *            You can optionally call {@link Subscriber#isUnsubscribed()} to
     *            check if you can stop emitting from the transition. If you do
     *            wish to terminate the Observable then call
     *            {@link Subscriber#unsubscribe()} and return anything (say
     *            {@code null} from the transition (as the next state which will
     *            not be used). You can also complete the Observable by calling
     *            {@link Subscriber#onCompleted} or {@link Subscriber#onError}
     *            from within the transition and return anything from the
     *            transition (will not be used). The transition should run
     *            synchronously so that completion of a call to the transition
     *            should also signify all emissions from that transition have
     *            been made.
     * @param completion
     *            defines activity that should happen based on the final state
     *            just before downstream <code>onCompleted()</code> is called.
     *            For example any buffered emissions in state could be emitted
     *            at this point. Don't call <code>observer.onCompleted()</code>
     *            as it is called for you after the action completes if and only
     *            if you return true from this function.
     * @param backpressureStrategy
     *            is applied to the emissions from one call of transition and
     *            should enforce backpressure.
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
            Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
            Func2<? super State, ? super Subscriber<Out>, Boolean> completion,
            BackpressureStrategy backpressureStrategy) {
        return TransformerStateMachine.<State, In, Out> create(initialStateFactory, transition,
                completion, backpressureStrategy);
    }

    /**
     * Returns a {@link Transformer} that allows processing of the source stream
     * to be defined in a state machine where transitions of the state machine
     * may also emit items to downstream that are buffered if necessary when
     * backpressure is requested. <code>flatMap</code> is part of the processing
     * chain so the source may experience requests for more items than are
     * strictly required by the endpoint subscriber. The backpressure strategy
     * used for emissions from the transition into the flatMap is
     * {@link BackpressureStrategy#BUFFER} which corresponds to
     * {@link Observable#onBackpressureBuffer}.
     * 
     * @param initialStateFactory
     *            the factory to create the initial state of the state machine.
     * @param transition
     *            defines state transitions and consequent emissions to
     *            downstream when an item arrives from upstream. The
     *            {@link Subscriber} is called with the emissions to downstream.
     *            You can optionally call {@link Subscriber#isUnsubscribed()} to
     *            check if you can stop emitting from the transition. If you do
     *            wish to terminate the Observable then call
     *            {@link Subscriber#unsubscribe()} and return anything (say
     *            {@code null} from the transition (as the next state which will
     *            not be used). You can also complete the Observable by calling
     *            {@link Subscriber#onCompleted} or {@link Subscriber#onError}
     *            from within the transition and return anything from the
     *            transition (will not be used). The transition should run
     *            synchronously so that completion of a call to the transition
     *            should also signify all emissions from that transition have
     *            been made.
     * @param completion
     *            defines activity that should happen based on the final state
     *            just before downstream <code>onCompleted()</code> is called.
     *            For example any buffered emissions in state could be emitted
     *            at this point. Don't call <code>observer.onCompleted()</code>
     *            as it is called for you after the action completes if and only
     *            if you return true from this function.
     * @param <State>
     *            the class representing the state of the state machine
     * @param <In>
     *            the input observable type
     * @param <Out>
     *            the output observable type
     * @throws NullPointerException
     *             if {@code initialStateFactory} or {@code transition},or
     *             {@code completion} is null
     * @return a backpressure supporting transformer that implements the state
     *         machine specified by the parameters
     */
    public static <State, In, Out> Transformer<In, Out> stateMachine(
            Func0<? extends State> initialStateFactory,
            Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
            Func2<? super State, ? super Subscriber<Out>, Boolean> completion) {
        return TransformerStateMachine.<State, In, Out> create(initialStateFactory, transition,
                completion, BackpressureStrategy.BUFFER);
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
     * is that the source and other are already ordered. This transformer
     * supports backpressure and its inputs must also support backpressure.
     * 
     * @param other
     *            the other already ordered observable
     * @param comparator
     *            the ordering to use
     * @param <T>
     *            the generic type of the objects being compared
     * @return merged and ordered observable
     */
    public static final <T> Transformer<T, T> orderedMergeWith(final Observable<? extends T> other,
            final Comparator<? super T> comparator) {
        @SuppressWarnings("unchecked")
        Collection<Observable<? extends T>> collection = (Collection<Observable<? extends T>>) (Collection<?>) Arrays
                .asList(other);
        return orderedMergeWith(collection, comparator);
    }

    /**
     * Returns the source {@link Observable} merged with all of the other
     * observables using the given {@link Comparator} for order. A precondition
     * is that the source and other are already ordered. This transformer
     * supports backpressure and its inputs must also support backpressure.
     * 
     * @param others
     *            a collection of already ordered observables to merge with
     * @param comparator
     *            the ordering to use
     * @param <T>
     *            the generic type of the objects being compared
     * @return merged and ordered observable
     */
    public static final <T> Transformer<T, T> orderedMergeWith(
            final Collection<Observable<? extends T>> others,
            final Comparator<? super T> comparator) {
        return new Transformer<T, T>() {

            @Override
            public Observable<T> call(Observable<T> source) {
                List<Observable<? extends T>> collection = new ArrayList<Observable<? extends T>>();
                collection.add(source);
                collection.addAll(others);
                return OrderedMerge.<T> create(collection, comparator, false);
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
     * @param isEmpty
     *            indicates that the collection is empty
     * @param <T>
     *            generic type of source observable
     * @param <R>
     *            collection type emitted by transformed Observable
     * @return transformer as above
     */
    public static <T, R> Transformer<T, R> collectWhile(final Func0<R> factory,
            final Action2<? super R, ? super T> collect,
            final Func2<? super R, ? super T, Boolean> condition,
            final Func1<? super R, Boolean> isEmpty) {
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
        Func2<R, Observer<R>, Boolean> completionAction = new Func2<R, Observer<R>, Boolean>() {
            @Override
            public Boolean call(R collection, Observer<R> observer) {
                if (!isEmpty.call(collection)) {
                    observer.onNext(collection);
                }
                return true;
            }
        };
        return Transformers.stateMachine(factory, transition, completionAction);
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

    public static <T, R extends Iterable<?>> Transformer<T, R> collectWhile(final Func0<R> factory,
            final Action2<? super R, ? super T> collect,
            final Func2<? super R, ? super T, Boolean> condition) {
        Func1<R, Boolean> isEmpty = new Func1<R, Boolean>() {
            @Override
            public Boolean call(R collection) {
                return !collection.iterator().hasNext();
            }
        };
        return collectWhile(factory, collect, condition, isEmpty);
    }

    /**
     * Returns a {@link Transformer} that applied to a source {@link Observable}
     * calls the given action on the {@code n}th onNext emission.
     * 
     * @param n
     *            the 1-based count of onNext to do the action on
     * @param action
     *            is performed on {@code n}th onNext.
     * @param <T>
     *            the generic type of the Observable being transformed
     * @return Transformer that applied to a source Observable calls the given
     *         action on the nth onNext emission.
     */
    public static <T> Transformer<T, T> doOnNext(final int n, final Action1<? super T> action) {
        return new Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> o) {
                return o.lift(OperatorDoOnNth.create(action, n));
            }
        };
    }

    /**
     * Returns a {@link Transformer} that applied to a source {@link Observable}
     * calls the given action on the first onNext emission.
     * 
     * @param action
     *            is performed on first onNext
     * @param <T>
     *            the generic type of the Observable being transformed
     * @return Transformer that applied to a source Observable calls the given
     *         action on the first onNext emission.
     */
    public static <T> Transformer<T, T> doOnFirst(final Action1<? super T> action) {
        return doOnNext(1, action);
    }

    public static <R, T> Transformer<T, R> ignoreElementsThen(final Observable<R> next) {
        return new Transformer<T, R>() {

            @SuppressWarnings("unchecked")
            @Override
            public Observable<R> call(Observable<T> source) {
                return ((Observable<R>) (Observable<?>) source.ignoreElements()).concatWith(next);
            }
        };
    }

    public static <T> Transformer<String, String> split(final String pattern) {
        return TransformerStringSplit.split(pattern);
    }

    /**
     * Decodes a stream of multibyte chunks into a stream of strings that works
     * on infinite streams and handles when a multibyte character spans two
     * chunks. This method allows for more control over how malformed and
     * unmappable characters are handled.
     * <p>
     * <img width="640" src=
     * "https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/St.decode.png"
     * alt="">
     * 
     * @param charsetDecoder
     *            decodes the bytes into strings
     * @return the Observable returning a stream of decoded strings
     */
    public static Transformer<byte[], String> decode(final CharsetDecoder charsetDecoder) {
        return TransformerDecode.decode(charsetDecoder);
    }

    public static <T> Transformer<T, T> limitSubscribers(AtomicInteger subscriberCount,
            int maxSubscribers) {
        return new TransformerLimitSubscribers<T>(subscriberCount, maxSubscribers);
    }

    public static <T> Transformer<T, T> limitSubscribers(int maxSubscribers) {
        return new TransformerLimitSubscribers<T>(new AtomicInteger(), maxSubscribers);
    }

    public static <T> Transformer<T, T> cache(final long duration, final TimeUnit unit,
            final Worker worker) {
        return new Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> o) {
                return Obs.cache(o, duration, unit, worker);
            }
        };
    }

    public static <T> Transformer<T, T> sampleFirst(final long duration, final TimeUnit unit) {
        return sampleFirst(duration, unit, Schedulers.computation());
    }

    public static <T> Transformer<T, T> sampleFirst(final long duration, final TimeUnit unit,
            final Scheduler scheduler) {
        if (duration <= 0) {
            throw new IllegalArgumentException("duration must be > 0");
        }
        return new Transformer<T, T>() {

            @Override
            public Observable<T> call(Observable<T> source) {
                return source.lift(new OperatorSampleFirst<T>(duration, unit, scheduler));
            }
        };
    }
}