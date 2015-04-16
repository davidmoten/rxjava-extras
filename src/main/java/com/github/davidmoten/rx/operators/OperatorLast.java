package com.github.davidmoten.rx.operators;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;

public class OperatorLast<T> implements Operator<T, T> {

    private static class Holder {
        static final OperatorLast<Object> INSTANCE = new OperatorLast<Object>();
    }

    @SuppressWarnings("unchecked")
    public static <T> OperatorLast<T> create() {
        return (OperatorLast<T>) Holder.INSTANCE;
    }

    private OperatorLast() {

    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(child);
        child.setProducer(new Producer() {

            @Override
            public void request(long n) {
                parent.requestMore(n);
            }
        });
        child.add(parent);
        return parent;
    }

    private static class ParentSubscriber<T> extends Subscriber<T> {

        private static enum State {
            NOT_REQUESTED_NOT_COMPLETED, NOT_REQUESTED_COMPLETED, REQUESTED_NOT_COMPLETED, REQUESTED_COMPLETED;
        }

        private static final Object ABSENT = new Object();

        private final Subscriber<? super T> child;
        private T value = (T) ABSENT;

        private final AtomicReference<State> state = new AtomicReference<State>(
                State.NOT_REQUESTED_NOT_COMPLETED);

        ParentSubscriber(Subscriber<? super T> child) {
            this.child = child;
        }

        void requestMore(long n) {
            if (n > 0) {
                if (state.compareAndSet(State.NOT_REQUESTED_NOT_COMPLETED,
                        State.REQUESTED_NOT_COMPLETED)) {
                    request(Long.MAX_VALUE);
                } else if (state.compareAndSet(State.NOT_REQUESTED_COMPLETED,
                        State.REQUESTED_COMPLETED)) {
                    emit();
                }
            }
        }

        @Override
        public void onCompleted() {
            if (value == ABSENT) {
                onError(new RuntimeException("last cannot be called on an empty stream"));
                return;
            }

            if (state.compareAndSet(State.REQUESTED_NOT_COMPLETED, State.REQUESTED_COMPLETED)) {
                emit();
            } else {
                state.compareAndSet(State.NOT_REQUESTED_NOT_COMPLETED,
                        State.NOT_REQUESTED_COMPLETED);
            }
        }

        private void emit() {
            if (isUnsubscribed()) {
                // release for gc
                value = null;
                return;
            }
            // synchronize to ensure that value is safely published
            synchronized (this) {
                child.onNext(value);
                // release for gc
                value = null;
            }
            if (!isUnsubscribed())
                child.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            value = t;
        }

    }

}
