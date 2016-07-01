package com.github.davidmoten.rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.internal.operators.NotificationLite;

public final class OperatorDematerializeLite<T> implements Operator<T, Object> {
    /** Lazy initialization via inner-class holder. */
    private static final class Holder {
        /** A singleton instance. */
        static final OperatorDematerializeLite<Object> INSTANCE = new OperatorDematerializeLite<Object>();
    }

    /**
     * @return a singleton instance of this stateless operator.
     */
    @SuppressWarnings({ "rawtypes" })
    public static OperatorDematerializeLite instance() {
        return Holder.INSTANCE; // using raw types because the type inference is
                                // not good enough
    }

    OperatorDematerializeLite() {
    }

    @Override
    public Subscriber<? super Object> call(final Subscriber<? super T> child) {
        return new Subscriber<Object>(child) {
            /** Do not send two onCompleted events. */
            boolean terminated;

            @Override
            public void onNext(Object t) {
                if (NotificationLite.instance().isError(t)) {
                    onError(NotificationLite.instance().getError(t));
                } else if (NotificationLite.instance().isCompleted(t)) {
                    onCompleted();
                } else if (!terminated) {
                    child.onNext(NotificationLite.<T> instance().getValue(t));
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!terminated) {
                    terminated = true;
                    child.onError(e);
                }
            }

            @Override
            public void onCompleted() {
                if (!terminated) {
                    terminated = true;
                    child.onCompleted();
                }
            }

        };
    }

}
