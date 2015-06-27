package com.github.davidmoten.rx.operators;

import java.util.LinkedList;
import java.util.Queue;

import rx.Notification;
import rx.Observable;
import rx.Observable.Transformer;
import rx.Observer;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;

public final class TransformerWithState<State, In, Out> implements Transformer<In, Out> {

    private final Func0<State> initialState;
    private final Func4<State, In, Boolean, Observer<Out>, State> transition;

    private TransformerWithState(Func0<State> initialState,
            Func4<State, In, Boolean, Observer<Out>, State> transition) {
        this.initialState = initialState;
        this.transition = transition;
    }

    public static <State, In, Out> Transformer<In, Out> create(Func0<State> initialState,
            Func4<State, In, Boolean, Observer<Out>, State> transition) {
        return new TransformerWithState<State, In, Out>(initialState, transition);
    }

    @Override
    public Observable<Out> call(Observable<In> source) {
        StateWithNotifications<State, Out> initial = new StateWithNotifications<State, Out>(
                initialState.call());
        return source.materialize()
        // do state transitions and record notifications
                .scan(initial, transformStateAndRecordNotifications())
                // use flatMap to emit notification values
                .flatMap(emitNotifications());
    }

    private Func2<StateWithNotifications<State, Out>, Notification<In>, StateWithNotifications<State, Out>> transformStateAndRecordNotifications() {
        return new Func2<StateWithNotifications<State, Out>, Notification<In>, StateWithNotifications<State, Out>>() {
            @Override
            public StateWithNotifications<State, Out> call(StateWithNotifications<State, Out> se,
                    Notification<In> in) {
                Recorder<Out> recorder = new Recorder<Out>();
                if (in.isOnError()) {
                    recorder.onError(in.getThrowable());
                    return new StateWithNotifications<State, Out>(se.state, recorder.notifications);
                } else {
                    In value = in.isOnCompleted() ? null : in.getValue();
                    State state2 = transition.call(se.state, value, in.isOnCompleted(), recorder);
                    if (in.isOnCompleted())
                        recorder.onCompleted();
                    return new StateWithNotifications<State, Out>(state2, recorder.notifications);
                }
            }
        };
    }

    private Func1<StateWithNotifications<State, Out>, Observable<Out>> emitNotifications() {
        return new Func1<StateWithNotifications<State, Out>, Observable<Out>>() {
            @Override
            public Observable<Out> call(StateWithNotifications<State, Out> se) {
                return Observable.from(se.notifications).dematerialize();
            }
        };
    }

    private static final class StateWithNotifications<State, Out> {
        final State state;
        final Queue<Notification<Out>> notifications;

        StateWithNotifications(State state, Queue<Notification<Out>> notifications) {
            this.state = state;
            this.notifications = notifications;
        }

        StateWithNotifications(State state) {
            this(state, new LinkedList<Notification<Out>>());
        }

    }

    private static final class Recorder<Out> implements Observer<Out> {

        final Queue<Notification<Out>> notifications = new LinkedList<Notification<Out>>();

        @Override
        public void onCompleted() {
            notifications.add(Notification.<Out> createOnCompleted());
        }

        @Override
        public void onError(Throwable e) {
            notifications.add(Notification.<Out> createOnError(e));

        }

        @Override
        public void onNext(Out t) {
            notifications.add(Notification.<Out> createOnNext(t));
        }

    }

}
