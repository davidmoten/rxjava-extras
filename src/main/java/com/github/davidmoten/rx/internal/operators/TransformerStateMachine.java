package com.github.davidmoten.rx.internal.operators;

import java.util.LinkedList;
import java.util.Queue;

import com.github.davidmoten.util.Preconditions;

import rx.Notification;
import rx.Observable;
import rx.Observable.Transformer;
import rx.Observer;
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
    private final Func3<? super State, ? super In, ? super StateMachineObserver<Out>, ? extends State> transition;
    private final Action2<? super State, ? super StateMachineObserver<Out>> completionAction;

    private TransformerStateMachine(Func0<State> initialState,
            Func3<? super State, ? super In, ? super StateMachineObserver<Out>, ? extends State> transition,
            Action2<? super State, ? super StateMachineObserver<Out>> completionAction) {
        Preconditions.checkNotNull(initialState);
        Preconditions.checkNotNull(transition);
        Preconditions.checkNotNull(completionAction);
        this.initialState = initialState;
        this.transition = transition;
        this.completionAction = completionAction;
    }

    public static <State, In, Out> Transformer<In, Out> create(Func0<State> initialState,
            Func3<? super State, ? super In, ? super StateMachineObserver<Out>, ? extends State> transition,
            Action2<? super State, ? super StateMachineObserver<Out>> completionAction) {
        return new TransformerStateMachine<State, In, Out>(initialState, transition,
                completionAction);
    }

    public interface StateMachineObserver<T> extends Observer<T> {
        void unsubscribe();

        boolean isUnsubscribed();
    }

    @Override
    public Observable<Out> call(final Observable<In> source) {

        return Observable.defer(new Func0<Observable<Out>>() {

            @Override
            public Observable<Out> call() {
                final Subscription subscription = Subscriptions.empty();
                final StateWithNotifications<State, Out> initial = new StateWithNotifications<State, Out>(
                        initialState.call());
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
            public StateWithNotifications<State, Out> call(StateWithNotifications<State, Out> sn,
                    Notification<In> in) {
                Recorder<Out> recorder = new Recorder<Out>(subscription);
                if (in.isOnError()) {
                    recorder.onError(in.getThrowable());
                    return new StateWithNotifications<State, Out>(sn.state, recorder.notifications,
                            recorder.size);
                } else if (in.isOnCompleted()) {
                    completionAction.call(sn.state, recorder);
                    recorder.onCompleted();
                    return new StateWithNotifications<State, Out>((State) null,
                            recorder.notifications, recorder.size);
                } else {
                    State nextState = transition.call(sn.state, in.getValue(), recorder);
                    return new StateWithNotifications<State, Out>(nextState, recorder.notifications,
                            recorder.size);
                }
            }
        };
    }

    private Func1<StateWithNotifications<State, Out>, Observable<Out>> emitNotifications() {
        return new Func1<StateWithNotifications<State, Out>, Observable<Out>>() {
            @Override
            public Observable<Out> call(StateWithNotifications<State, Out> sn) {
                // enable flatMap fast path with ScalarSynchronousObservable if
                // there is only one value in notifications
                if (sn.size == 2) {
                    Notification<Out> first = sn.notifications.poll();
                    Notification<Out> second = sn.notifications.poll();
                    if (first.hasValue() && second.isOnCompleted()) {
                        return Observable.just(first.getValue());
                    }
                }
                return Observable.from(sn.notifications).dematerialize();
            }
        };
    }

    private static final class StateWithNotifications<State, Out> {
        final State state;
        final Queue<Notification<Out>> notifications;
        final int size;

        StateWithNotifications(State state, Queue<Notification<Out>> notifications, int size) {
            this.state = state;
            this.notifications = notifications;
            this.size = size;
        }

        StateWithNotifications(State state) {
            this(state, new LinkedList<Notification<Out>>(), 0);
        }

    }

    private static final class Recorder<Out> implements StateMachineObserver<Out> {

        final Queue<Notification<Out>> notifications = new LinkedList<Notification<Out>>();

        int size = 0;

        private final Subscription subscription;

        Recorder(Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void onCompleted() {
            add(Notification.<Out> createOnCompleted());
        }

        @Override
        public void onError(Throwable e) {
            add(Notification.<Out> createOnError(e));
        }

        @Override
        public void onNext(Out t) {
            add(Notification.<Out> createOnNext(t));
        }

        @Override
        public void unsubscribe() {
            subscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return subscription.isUnsubscribed();
        }

        private void add(Notification<Out> notification) {
            if (!subscription.isUnsubscribed()) {
                notifications.add(notification);
                size++;
            }
        }

    }

}
