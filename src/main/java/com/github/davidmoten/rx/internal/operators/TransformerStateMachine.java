package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicReference;

import com.github.davidmoten.util.Preconditions;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
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
                final Subscription subscription = Subscriptions.empty();
                final StateWithNotifications<State, Out> initial = new StateWithNotifications<State, Out>(
                        Observable.just(initialState.call()));
                return source.materialize()
                        // do state transitions and record notifications
                        .scan(initial, transformStateAndRecordNotifications(subscription))
                        // use concatMap to emit notification values
                        .flatMap(emitNotifications())
                        // unsubscribe action
                        .doOnUnsubscribe(unsubscribe(subscription));
            }

        });
    }

    private static Action0 unsubscribe(final Subscription subscription) {
        return new Action0() {
            @Override
            public void call() {
                subscription.unsubscribe();
            }
        };
    }

    private Func2<StateWithNotifications<State, Out>, Notification<In>, StateWithNotifications<State, Out>> transformStateAndRecordNotifications(
            final Subscription subscription) {
        return new Func2<StateWithNotifications<State, Out>, Notification<In>, StateWithNotifications<State, Out>>() {
            @Override
            public StateWithNotifications<State, Out> call(
                    final StateWithNotifications<State, Out> sn, final Notification<In> in) {
                final AtomicReference<State> nextStateRef = new AtomicReference<State>();
                final Observable<State> nextState = Observable
                        .defer(new Func0<Observable<State>>() {
                    @Override
                    public Observable<State> call() {
                        return Observable.just(nextStateRef.get());
                    }
                });
                Observable<Notification<Out>> emissions = createEmissions(sn, in, nextStateRef);
                return new StateWithNotifications<State, Out>(nextState, emissions);
            }

            private Observable<Notification<Out>> createEmissions(
                    final StateWithNotifications<State, Out> sn, final Notification<In> in,
                    final AtomicReference<State> nextStateRef) {
                // block to get this state to decouple this transition from the
                // last
                final State state = sn.state.toBlocking().single();
                return Observable.create(new OnSubscribe<Notification<Out>>() {

                    @Override
                    public void call(rx.Subscriber<? super Notification<Out>> subscriber) {
                        Subscriber<Out> wrapped = wrap(subscriber);
                        if (in.isOnError()) {
                            wrapped.onError(in.getThrowable());
                        } else if (in.isOnCompleted()) {
                            try {
                                completionAction.call(state, wrapped);
                                wrapped.onCompleted();
                            } catch (RuntimeException e) {
                                wrapped.onError(e);
                            }
                        } else {
                            try {
                                State s = transition.call(state, in.getValue(), wrapped);
                                wrapped.onCompleted();
                                nextStateRef.set(s);
                            } catch (RuntimeException e) {
                                wrapped.onError(e);
                            }
                        }
                    }

                }).onBackpressureBuffer();
            }

        };
    }

    private static <Out> Subscriber<Out> wrap(
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
        final Observable<State> state;
        final Observable<Notification<Out>> notifications;

        StateWithNotifications(Observable<State> state,
                Observable<Notification<Out>> notifications) {
            this.state = state;
            this.notifications = notifications;
        }

        StateWithNotifications(Observable<State> state) {
            this(state, Observable.<Notification<Out>> empty());
        }

    }

}
