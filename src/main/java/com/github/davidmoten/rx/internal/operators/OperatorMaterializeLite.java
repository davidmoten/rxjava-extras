package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicLong;

import com.github.davidmoten.rx.util.BackpressureUtils;

import rx.Observable.Operator;
import rx.internal.operators.NotificationLite;
import rx.Producer;
import rx.Subscriber;
import rx.plugins.RxJavaPlugins;

/**
 * Turns all of the notifications from an Observable into {@code onNext} emissions, and marks
 * them with their original notification types within {@link NotificationLite} objects.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/materialize.png" alt="">
 * <p>
 * See <a href="http://msdn.microsoft.com/en-us/library/hh229453.aspx">here</a> for the Microsoft Rx equivalent.
 */
public final class OperatorMaterializeLite<T> implements Operator<Object, T> {

    /** Lazy initialization via inner-class holder. */
    private static final class Holder {
        /** A singleton instance. */
        static final OperatorMaterializeLite<Object> INSTANCE = new OperatorMaterializeLite<Object>();
    }

    /**
     * @return a singleton instance of this stateless operator.
     */
    @SuppressWarnings("unchecked")
    public static <T> OperatorMaterializeLite<T> instance() {
        return (OperatorMaterializeLite<T>) Holder.INSTANCE;
    }

    OperatorMaterializeLite() {
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super Object> child) {
        final ParentSubscriber<T> parent = new ParentSubscriber<T>(child);
        child.add(parent);
        child.setProducer(new Producer() {
            @Override
            public void request(long n) {
                if (n > 0) {
                    parent.requestMore(n);
                }
            }
        });
        return parent;
    }

    private static class ParentSubscriber<T> extends Subscriber<T> {

        private final Subscriber<? super Object> child;

        private volatile Object terminalNotification;
        
        // guarded by this
        private boolean busy = false;
        // guarded by this
        private boolean missed = false;

        private final AtomicLong requested = new AtomicLong();

        ParentSubscriber(Subscriber<? super Object> child) {
            this.child = child;
        }

        @Override
        public void onStart() {
            request(0);
        }

        void requestMore(long n) {
            BackpressureUtils.getAndAddRequest(requested, n);
            request(n);
            drain();
        }

        @Override
        public void onCompleted() {
            terminalNotification = NotificationLite.instance().completed();
            drain();
        }

        @Override
        public void onError(Throwable e) {
            terminalNotification = NotificationLite.instance().error(e);
            RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
            drain();
        }

        @Override
        public void onNext(T t) {
            child.onNext(NotificationLite.instance().next(t));
            decrementRequested();
        }

        private void decrementRequested() {
            // atomically decrement requested
            AtomicLong localRequested = this.requested;
            while (true) {
                long r = localRequested.get();
                if (r == Long.MAX_VALUE) {
                    // don't decrement if unlimited requested
                    return;
                } else if (localRequested.compareAndSet(r, r - 1)) {
                    return;
                }
            }
        }

        private void drain() {
            synchronized (this) {
                if (busy) {
                    // set flag to force extra loop if drain loop running
                    missed = true;
                    return;
                } 
            }
            // drain loop
            final AtomicLong localRequested = this.requested;
            while (!child.isUnsubscribed()) {
                Object tn;
                tn = terminalNotification;
                if (tn != null) {
                    if (localRequested.get() > 0) {
                        // allow tn to be GC'd after the onNext call
                        terminalNotification = null;
                        // emit the terminal notification
                        child.onNext(tn);
                        if (!child.isUnsubscribed()) {
                            child.onCompleted();
                        }
                        // note that we leave busy=true here
                        // which will prevent further drains
                        return;
                    }
                }
                // continue looping if drain() was called while in
                // this loop
                synchronized (this) {
                    if (!missed) {
                        busy = false;
                        return;
                    }
                }
            }
        }
    }

}
