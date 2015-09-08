package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicReference;

import com.github.davidmoten.util.Preconditions;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;

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
                final StateWithNotifications<State, Out> initial = new StateWithNotifications<State, Out>(
                        new AtomicReference<State>(initialState.call()));
                return source.materialize()
                        // do state transitions and record notifications
                        .scan(initial,
                                transformStateAndHandleEmissions(transition, completionAction))
                        // use concatMap to emit notification values
                        .flatMap(TransformerStateMachine.<State, Out> emitNotifications());
            }

        });
    }

    private static <State, In, Out> Func2<StateWithNotifications<State, Out>, Notification<In>, StateWithNotifications<State, Out>> transformStateAndHandleEmissions(
            final Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
            final Action2<? super State, ? super Subscriber<Out>> completionAction) {
        return new Func2<StateWithNotifications<State, Out>, Notification<In>, StateWithNotifications<State, Out>>() {
            @Override
            public StateWithNotifications<State, Out> call(
                    final StateWithNotifications<State, Out> sn, final Notification<In> in) {
                final AtomicReference<State> nextState = new AtomicReference<State>();
                Observable<Notification<Out>> emissions = Observable
                        .create(new EmissionsOnSubscribe<State, In, Out>(transition,
                                completionAction, sn, in, nextState))
                        // because this observable will be fed into flatMap we
                        // need to buffer emissions if need be to support
                        // backpressure
                        .onBackpressureBuffer();

                return new StateWithNotifications<State, Out>(nextState, emissions);
            }

        };
    }

    private static class EmissionsOnSubscribe<State, In, Out>
            implements OnSubscribe<Notification<Out>> {

        private final Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition;
        private final Action2<? super State, ? super Subscriber<Out>> completionAction;
        private final StateWithNotifications<State, Out> sn;
        private final Notification<In> in;
        private final AtomicReference<State> nextState;

        public EmissionsOnSubscribe(
                Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
                Action2<? super State, ? super Subscriber<Out>> completionAction,
                StateWithNotifications<State, Out> sn, Notification<In> in,
                AtomicReference<State> nextState) {
            this.transition = transition;
            this.completionAction = completionAction;
            this.sn = sn;
            this.in = in;
            this.nextState = nextState;
        }

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

    }

    private static <Out> Subscriber<Out> materialize(
            final rx.Subscriber<? super Notification<Out>> subscriber) {
        return new Subscriber<Out>(subscriber) {

            boolean finished = false;

            @Override
            public void onCompleted() {
                if (!finished) {
                    finished = true;
                    subscriber.onNext(Notification.<Out> createOnCompleted());
                    subscriber.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!finished) {
                    finished = true;
                    subscriber.onNext(Notification.<Out> createOnError(e));
                    subscriber.onCompleted();
                }
            }

            @Override
            public void onNext(Out t) {
                if (!finished) {
                    subscriber.onNext(Notification.createOnNext(t));
                }
            }
        };
    }

    private static <State, Out> Func1<StateWithNotifications<State, Out>, Observable<Out>> emitNotifications() {
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
