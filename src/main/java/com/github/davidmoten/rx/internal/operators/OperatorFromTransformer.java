package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicReference;

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
        final AtomicReference<Subscriber<? super T>> ref = new AtomicReference<Subscriber<? super T>>();
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(ref);
        Observable<T> middle = Observable.create(new OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> sub) {
                ref.set(sub);
                sub.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        parent.requestMore(n);
                    }
                });
            }
        });
        operation.call(middle).unsafeSubscribe(subscriber);
        subscriber.add(parent);
        return parent;
    }

    static final class ParentSubscriber<T> extends Subscriber<T> {

        private final AtomicReference<Subscriber<? super T>> ref;

        public ParentSubscriber(AtomicReference<Subscriber<? super T>> ref) {
            this.ref = ref;
        }

        void requestMore(long n) {
            request(n);
        }

        @Override
        public void onCompleted() {
            ref.get().onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            ref.get().onError(e);
        }

        @Override
        public void onNext(T t) {
            ref.get().onNext(t);
        }

    }

}
