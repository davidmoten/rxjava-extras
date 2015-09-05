package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicReference;

import com.github.davidmoten.rx.Actions;
import com.github.davidmoten.util.Preconditions;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.subscriptions.Subscriptions;

public final class TransformerStateMachine<State, In, Out> implements Transformer<In, Out> {

    private final Func0<State> initialState;
    private final Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition;
    private final Action2<? super State, ? super Subscriber<Out>> completionAction;

    private TransformerStateMachine(Func0<State> initialState,
            Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
            Action2<? super State, ? super Subscriber<Out>> completionAction) {
        Preconditions.checkNotNull(initialState);
        Preconditions.checkNotNull(transition);
        Preconditions.checkNotNull(completionAction);
        this.initialState = initialState;
        this.transition = transition;
        this.completionAction = completionAction;
    }

    public static <State, In, Out> Transformer<In, Out> create(Func0<State> initialState,
            Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
            Action2<? super State, ? super Subscriber<Out>> completionAction) {
        return new TransformerStateMachine<State, In, Out>(initialState, transition,
                completionAction);
    }

    @Override
    public Observable<Out> call(final Observable<In> source) {

        return Observable.defer(new Func0<Observable<Out>>() {

            @Override
            public Observable<Out> call() {
                // make a subscription that will be used in the transition to
                // stop emitting if need be
                final Subscription subscription = Subscriptions.empty();
                final StateWithNotifications<State, Out> initial = new StateWithNotifications<State, Out>(
                        new AtomicReference<State>(initialState.call()));
                return source.materialize()
                        // do state transitions and record notifications
                        .scan(initial, transformStateAndHandleEmissions(subscription))
                        // use concatMap to emit notification values
                        .flatMap(emitNotifications())
                        // unsubscribe action
                        .doOnUnsubscribe(Actions.unsubscribe(subscription));
            }

        });
    }

    private Func2<StateWithNotifications<State, Out>, Notification<In>, StateWithNotifications<State, Out>> transformStateAndHandleEmissions(
            final Subscription subscription) {
        return new Func2<StateWithNotifications<State, Out>, Notification<In>, StateWithNotifications<State, Out>>() {
            @Override
            public StateWithNotifications<State, Out> call(
                    final StateWithNotifications<State, Out> sn, final Notification<In> in) {
                final AtomicReference<State> nextState = new AtomicReference<State>();
                Observable<Notification<Out>> emissions = Observable
                        .create(new OnSubscribe<Notification<Out>>() {

                    @Override
                    public void call(rx.Subscriber<? super Notification<Out>> subscriber) {
                        State state = sn.state.get();
                        Subscriber<Out> materialize = materialize(subscriber);
                        if (in.isOnError()) {
                            materialize.onError(in.getThrowable());
                        } else if (in.isOnCompleted()) {
                            try {
                                completionAction.call(state, materialize);
                                materialize.onCompleted();
                            } catch (RuntimeException e) {
                                materialize.onError(e);
                            }
                        } else {
                            try {
                                nextState.set(transition.call(state, in.getValue(), materialize));
                                materialize.onCompleted();
                            } catch (RuntimeException e) {
                                materialize.onError(e);
                            }
                        }
                    }
                })
                        // because this observable will be fed into flatMap we
                        // need to bummer emissions if need be to support
                        // backpressure
                        .onBackpressureBuffer();

                return new StateWithNotifications<State, Out>(nextState, emissions);
            }

        };
    }

    private static <Out> Subscriber<Out> materialize(
            final rx.Subscriber<? super Notification<Out>> subscriber) {
        return new Subscriber<Out>(subscriber) {

            @Override
            public void onCompleted() {
                subscriber.onNext(Notification.<Out> createOnCompleted());
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onNext(Notification.<Out> createOnError(e));
                subscriber.onCompleted();
            }

            @Override
            public void onNext(Out t) {
                subscriber.onNext(Notification.createOnNext(t));
            }
        };
    }

    private Func1<StateWithNotifications<State, Out>, Observable<Out>> emitNotifications() {
        return new Func1<StateWithNotifications<State, Out>, Observable<Out>>() {
            @Override
            public Observable<Out> call(StateWithNotifications<State, Out> sn) {
                return sn.notifications.dematerialize();
            }
        };
    }

    private static final class StateWithNotifications<State, Out> {
        final AtomicReference<State> state;
        final Observable<Notification<Out>> notifications;

        StateWithNotifications(AtomicReference<State> state,
                Observable<Notification<Out>> notifications) {
            this.state = state;
            this.notifications = notifications;
        }

        StateWithNotifications(AtomicReference<State> state) {
            this(state, Observable.<Notification<Out>> empty());
        }

    }

}
