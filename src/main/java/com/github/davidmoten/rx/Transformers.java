package com.github.davidmoten.rx;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func4;

import com.github.davidmoten.rx.operators.OperatorBufferEmissions;
import com.github.davidmoten.rx.operators.OperatorFromTransformer;
import com.github.davidmoten.rx.operators.TransformerWithState;
import com.github.davidmoten.rx.util.MapWithIndex;
import com.github.davidmoten.rx.util.MapWithIndex.Indexed;

public final class Transformers {

    public static <T, R> Operator<R, T> toOperator(Func1<Observable<T>, Observable<R>> function) {
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

    public static <T extends Comparable<T>> Transformer<T, T> sort() {
        return new Transformer<T, T>() {

            @Override
            public Observable<T> call(Observable<T> o) {
                return o.toSortedList().flatMapIterable(Functions.<List<T>> identity());
            }
        };
    }

    public static <T extends Comparable<T>> Transformer<T, T> sort(final Comparator<T> comparator) {
        return new Transformer<T, T>() {

            @Override
            public Observable<T> call(Observable<T> o) {
                return o.toSortedList(Functions.toFunc2(comparator)).flatMapIterable(
                        Functions.<List<T>> identity());
            }
        };
    }

    public static <T> Transformer<T, Set<T>> toSet() {
        return new Transformer<T, Set<T>>() {

            @Override
            public Observable<Set<T>> call(Observable<T> o) {
                return o.toList().map(new Func1<List<T>, Set<T>>() {

                    @Override
                    public Set<T> call(List<T> list) {
                        return Collections.unmodifiableSet(new HashSet<T>(list));
                    }
                });
            }
        };
    }

    public static <T> Transformer<T, Indexed<T>> mapWithIndex() {
        return MapWithIndex.instance();
    }

    public static <T> Transformer<T, T> bufferEmissions(final Scheduler scheduler) {
        return new Transformer<T, T>() {

            @Override
            public Observable<T> call(Observable<T> o) {
                return o.lift(new OperatorBufferEmissions<T>(scheduler));
            }
        };
    }

    public static <State, In, Out> Transformer<In, Out> withState(Func0<State> initialState,
            Func4<State, In, Boolean, Observer<Out>, State> transition) {
        return TransformerWithState.<State, In, Out> create(initialState, transition);
    }

    @SuppressWarnings("unchecked")
    public static <T> Transformer<T, T> bufferEmissions() {
        return (Transformer<T, T>) BufferEmissionsHolder.INSTANCE;
    }

    // holder lazy singleton pattern
    private static class BufferEmissionsHolder {
        static Transformer<Object, Object> INSTANCE = bufferEmissions(null);
    }

}
