package com.github.davidmoten.rx.operators;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Func2;

public class OperatorReduce<T, R> implements Operator<R, T> {

    public static <T> OperatorReduce<T, T> create(Func2<T, T, T> reduction) {
        return new OperatorReduce<T, T>(reduction);
    }

    public static <T, R> OperatorReduce<T, R> create(R initialValue,
            Func2<R, ? super T, R> reduction) {
        return new OperatorReduce<T, R>(initialValue, reduction);
    }

    private final Func2<R, ? super T, R> reduction;
    private final R initialValue;
    private static final Object NO_INITIAL_VALUE = new Object();

    private OperatorReduce(R initialValue, Func2<R, ? super T, R> reduction) {
        this.reduction = reduction;
        this.initialValue = initialValue;
    }

    @SuppressWarnings("unchecked")
    private OperatorReduce(Func2<R, ? super T, R> reduction) {
        // this does not throw a ClassCastException at runtime even though
        // NO_INITIAL_VALUE may not be of type R because R is a generic type and
        // its type is erased at runtime.
        this((R) NO_INITIAL_VALUE, reduction);
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super R> child) {
        final ParentSubscriber<T, R> parent = new ParentSubscriber<T, R>(child, reduction,
                initialValue);
        child.setProducer(new Producer() {

            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        });
        child.add(parent);
        return parent;
    }

    private static class ParentSubscriber<T, R> extends Subscriber<T> {

        private static enum State {
            NOT_REQUESTED_NOT_COMPLETED, NOT_REQUESTED_COMPLETED, REQUESTED_NOT_COMPLETED, REQUESTED_COMPLETED;
        }

        private final Subscriber<? super R> child;
        private final Func2<R, ? super T, R> reduction;
        private R value;

        private final AtomicReference<State> state = new AtomicReference<State>(
                State.NOT_REQUESTED_NOT_COMPLETED);

        ParentSubscriber(Subscriber<? super R> child, Func2<R, ? super T, R> reduction,
                R initialValue) {
            this.child = child;
            this.reduction = reduction;
            this.value = initialValue;
        }

        void requestMore(long n) {
            if (n > 0) {
                if (!state.compareAndSet(State.NOT_REQUESTED_NOT_COMPLETED,
                        State.REQUESTED_NOT_COMPLETED)) {
                    if (state.compareAndSet(State.NOT_REQUESTED_COMPLETED,
                            State.REQUESTED_COMPLETED)) {
                        emit();
                    }
                }
            }
        }

        @Override
        public void onCompleted() {
            if (state.compareAndSet(State.REQUESTED_NOT_COMPLETED, State.REQUESTED_COMPLETED)) {
                emit();
            } else {
                state.compareAndSet(State.NOT_REQUESTED_NOT_COMPLETED,
                        State.NOT_REQUESTED_COMPLETED);
            }
        }

        private void emit() {
            if (isUnsubscribed())
                return;
            // synchronize to ensure that value is safely published
            synchronized (this) {
                if (value == NO_INITIAL_VALUE)
                    throw new RuntimeException(
                            "reduce without an initial value expects at least two items");
                child.onNext(value);
                // release for gc
                value = null;
                if (!isUnsubscribed())
                    child.onCompleted();
            }
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onNext(T t) {
            if (value == NO_INITIAL_VALUE)
                value = (R) t;
            else
                value = reduction.call(value, t);
        }

    }

}
