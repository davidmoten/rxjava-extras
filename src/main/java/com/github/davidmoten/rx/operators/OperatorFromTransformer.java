package com.github.davidmoten.rx.operators;

import com.github.davidmoten.rx.subjects.PublishSubjectSingleSubscriber;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observers.Subscribers;
import rx.subjects.Subject;

/**
 * Converts an Transformer (a function converting one Observable into another)
 * into an {@link Operator}.
 * 
 * @param <R>
 *            to type
 * @param <T>
 *            from type
 */
public final class OperatorFromTransformer<R, T> implements Operator<R, T> {

    public static <R, T> Operator<R, T> toOperator(
            Func1<? super Observable<T>, ? extends Observable<R>> operation) {
        return new OperatorFromTransformer<R, T>(operation);
    }

    /**
     * The operation to convert.
     */
    private final Func1<? super Observable<T>, ? extends Observable<R>> operation;

    /**
     * Constructor.
     * 
     * @param operation
     *            to be converted into {@link Operator}
     */
    public OperatorFromTransformer(
            Func1<? super Observable<T>, ? extends Observable<R>> operation) {
        this.operation = operation;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super R> subscriber) {
        Subject<T, T> subject = PublishSubjectSingleSubscriber.create();
        Subscriber<T> result = Subscribers.from(subject);
        subscriber.add(result);
        operation.call(subject).unsafeSubscribe(subscriber);
        return result;
    }

}
