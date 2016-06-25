package com.github.davidmoten.rx.internal.operators;

import com.github.davidmoten.rx.util.BackpressureStrategy;
import com.github.davidmoten.util.Preconditions;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;

public final class TransformerStateMachine<State, In, Out> implements Transformer<In, Out> {

    private final Func0<? extends State> initialState;
    private final Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition;
    private final Func2<? super State, ? super Subscriber<Out>, Boolean> completion;
    private final BackpressureStrategy backpressureStrategy;
    private final int initialRequest;

    private TransformerStateMachine(Func0<? extends State> initialState,
            Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
            Func2<? super State, ? super Subscriber<Out>, Boolean> completion,
            BackpressureStrategy backpressureStrategy, int initialRequest) {
        Preconditions.checkNotNull(initialState);
        Preconditions.checkNotNull(transition);
        Preconditions.checkNotNull(completion);
        Preconditions.checkNotNull(backpressureStrategy);
        Preconditions.checkArgument(initialRequest > 0, "initialRequest must be greater than zero");
        this.initialState = initialState;
        this.transition = transition;
        this.completion = completion;
        this.backpressureStrategy = backpressureStrategy;
        this.initialRequest = initialRequest;
    }

    public static <State, In, Out> Transformer<In, Out> create(Func0<? extends State> initialState,
            Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
            Func2<? super State, ? super Subscriber<Out>, Boolean> completion,
            BackpressureStrategy backpressureStrategy, int initialRequest) {
        return new TransformerStateMachine<State, In, Out>(initialState, transition, completion,
                backpressureStrategy, initialRequest);
    }

    @Override
    public Observable<Out> call(final Observable<In> source) {
        // use defer so we can have a single state reference for each
        // subscription
        return Observable.defer(new Func0<Observable<Out>>() {
            @Override
            public Observable<Out> call() {
                Mutable<State> state = new Mutable<State>(initialState.call());
                return source.materialize()
                        // do state transitions and emit notifications
                        // use flatMap to emit notification values
                        .flatMap(execute(transition, completion, state, backpressureStrategy),
                                initialRequest)
                        // complete if we encounter an unsubscribed sentinel
                        .takeWhile(NOT_UNSUBSCRIBED)
                        // flatten notifications to a stream which will enable
                        // early termination from the state machine if desired
                        .dematerialize();
            }
        });
    }

    private static <State, Out, In> Func1<Notification<In>, Observable<Notification<Out>>> execute(
            final Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
            final Func2<? super State, ? super Subscriber<Out>, Boolean> completion,
            final Mutable<State> state, final BackpressureStrategy backpressureStrategy) {

        return new Func1<Notification<In>, Observable<Notification<Out>>>() {

            @Override
            public Observable<Notification<Out>> call(final Notification<In> in) {

                Observable<Notification<Out>> o = Observable
                        .create(new OnSubscribe<Notification<Out>>() {

                    @Override
                    public void call(Subscriber<? super Notification<Out>> subscriber) {
                        Subscriber<Out> w = wrap(subscriber);
                        if (in.hasValue()) {
                            state.value = transition.call(state.value, in.getValue(), w);
                            if (!subscriber.isUnsubscribed())
                                subscriber.onCompleted();
                            else {
                                // this is a special emission to indicate that the
                                // transition called unsubscribe. It will be
                                // filtered later.
                                subscriber.onNext(UnsubscribedNotificationHolder.<Out>unsubscribedNotification());
                            }
                        } else if (in.isOnCompleted()) {
                            if (completion.call(state.value, w) && !subscriber.isUnsubscribed()) {
                                w.onCompleted();
                            }
                        } else if (!subscriber.isUnsubscribed()) {
                            w.onError(in.getThrowable());
                        }
                    }
                });
                // because the observable we just created does not
                // support backpressure we need to apply a backpressure
                // handling operator. This operator is supplied by the
                // user.
                return applyBackpressure(o, backpressureStrategy);
            }

        };
    }
    
    private static final class UnsubscribedNotificationHolder {
        private static final Notification<Object> INSTANCE = Notification.createOnNext(null);
        
        @SuppressWarnings("unchecked")
        static <T> Notification<T> unsubscribedNotification() {
            return (Notification<T>) INSTANCE;
        }
    }

    private static <Out> Observable<Notification<Out>> applyBackpressure(
            Observable<Notification<Out>> o, final BackpressureStrategy backpressureStrategy) {
        if (backpressureStrategy == BackpressureStrategy.BUFFER)
            return o.onBackpressureBuffer();
        else if (backpressureStrategy == BackpressureStrategy.DROP)
            return o.onBackpressureDrop();
        else if (backpressureStrategy == BackpressureStrategy.LATEST)
            return o.onBackpressureLatest();
        else
            throw new IllegalArgumentException(
                    "backpressure strategy not supported: " + backpressureStrategy);
    }

    private static final Func1<Notification<?>, Boolean> NOT_UNSUBSCRIBED = new Func1<Notification<?>, Boolean>() {

        @Override
        public Boolean call(Notification<?> t) {
            return t != UnsubscribedNotificationHolder.unsubscribedNotification();
        }

    };

    private static final class Mutable<T> {
        T value;

        Mutable(T value) {
            this.value = value;
        }
    }

    private static <Out> NotificationSubscriber<Out> wrap(
            Subscriber<? super Notification<Out>> sub) {
        return new NotificationSubscriber<Out>(sub);
    }

    private static final class NotificationSubscriber<Out> extends Subscriber<Out> {

        private final Subscriber<? super Notification<Out>> sub;

        NotificationSubscriber(Subscriber<? super Notification<Out>> sub) {
            this.sub = sub;
            add(sub);
        }

        @Override
        public void onCompleted() {
            sub.onNext(Notification.<Out> createOnCompleted());
        }

        @Override
        public void onError(Throwable e) {
            sub.onNext(Notification.<Out> createOnError(e));
        }

        @Override
        public void onNext(Out t) {
            sub.onNext(Notification.createOnNext(t));
        }

    }

}
