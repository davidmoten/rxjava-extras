package com.github.davidmoten.rx.operators;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;

public class OperatorTakeLastOne<T> implements Operator<T, T> {

    private static class Holder {
        static final OperatorTakeLastOne<Object> INSTANCE = new OperatorTakeLastOne<Object>();
    }

    @SuppressWarnings("unchecked")
    public static <T> OperatorTakeLastOne<T> create() {
        return (OperatorTakeLastOne<T>) Holder.INSTANCE;
    }

    private OperatorTakeLastOne() {

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

        @SuppressWarnings("unchecked")
        // we can get away with this at runtime because of type erasure
        private T last = (T) ABSENT;

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
                last = null;
                return;
            }
            // synchronize to ensure that value is safely published
            synchronized (this) {
                if (last != ABSENT)
                    child.onNext(last);
                // release for gc
                last = null;
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
            last = t;
        }

    }

}
