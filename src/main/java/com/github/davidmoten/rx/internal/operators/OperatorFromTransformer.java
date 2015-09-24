package com.github.davidmoten.rx.internal.operators;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Func1;

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
        final ParentSubscriber<T> parent = new ParentSubscriber<T>();
        Observable<T> middle = Observable.create(new ForwarderOnSubscribe<T>(parent));
        subscriber.add(parent);
        operation.call(middle).unsafeSubscribe(subscriber);
        return parent;
    }

    static final class ForwarderOnSubscribe<T> implements OnSubscribe<T> {

        private final ParentSubscriber<T> parent;

        ForwarderOnSubscribe(ParentSubscriber<T> parent) {
            this.parent = parent;
        }

        @Override
        public void call(Subscriber<? super T> sub) {
            parent.subscriber = sub;
            sub.setProducer(new Producer() {

                @Override
                public void request(long n) {
                    parent.requestMore(n);
                }
            });
        }
    }

    static final class ParentSubscriber<T> extends Subscriber<T> {

        // TODO may not need to be volatile
        volatile Subscriber<? super T> subscriber;

        void requestMore(long n) {
            request(n);
        }

        @Override
        public void onCompleted() {
            subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            subscriber.onError(e);
        }

        @Override
        public void onNext(T t) {
            subscriber.onNext(t);
        }

    }

}
