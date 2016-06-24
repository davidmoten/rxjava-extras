package com.github.davidmoten.rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.internal.operators.NotificationLite;
import rx.internal.operators.OperatorMaterialize;

/**
 * Reverses the effect of {@link OperatorMaterialize} by transforming the
 * Notification objects emitted by a source Observable into the items or
 * notifications they represent.
 * <p>
 * <img width="640" src=
 * "https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/dematerialize.png"
 * alt="">
 * <p>
 * See <a href="http://msdn.microsoft.com/en-us/library/hh229047.aspx">here</a>
 * for the Microsoft Rx equivalent.
 * 
 * @param <T>
 *            the wrapped value type
 */
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
