package com.github.davidmoten.rx.operators;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observers.Subscribers;

import com.github.davidmoten.rx.subjects.SingleSubscribeSubject;

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

    public static <R, T> Operator<R, T> toOperator(Func1<Observable<T>, Observable<R>> operation) {
        return new OperatorFromTransformer<R, T>(operation);
    }

    /**
     * The operation to convert.
     */
    private final Func1<Observable<T>, Observable<R>> operation;

    /**
     * Constructor.
     * 
     * @param operation
     *            to be converted into {@link Operator}
     */
    public OperatorFromTransformer(Func1<Observable<T>, Observable<R>> operation) {
        this.operation = operation;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super R> subscriber) {
        SingleSubscribeSubject<T> subject = SingleSubscribeSubject.create();
        Subscriber<T> result = Subscribers.from(subject);
        subscriber.add(result);
        operation.call(subject).unsafeSubscribe(subscriber);
        return result;
    }

}
