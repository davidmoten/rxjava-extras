package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicReference;

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

    private final Func0<State> initialState;
    private final Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition;
    private final Func2<? super State, ? super Subscriber<Out>, Boolean> completion;

    private TransformerStateMachine(Func0<State> initialState,
            Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
            Func2<? super State, ? super Subscriber<Out>, Boolean> completion) {
        Preconditions.checkNotNull(initialState);
        Preconditions.checkNotNull(transition);
        Preconditions.checkNotNull(completion);
        this.initialState = initialState;
        this.transition = transition;
        this.completion = completion;
    }

    public static <State, In, Out> Transformer<In, Out> create(Func0<State> initialState,
            Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
            Func2<? super State, ? super Subscriber<Out>, Boolean> completion) {
        return new TransformerStateMachine<State, In, Out>(initialState, transition, completion);
    }

    @Override
    public Observable<Out> call(final Observable<In> source) {
        // use defer so we can have a single state reference for each
        // subscription
        return Observable.defer(new Func0<Observable<Out>>() {
            @Override
            public Observable<Out> call() {
                AtomicReference<State> state = new AtomicReference<State>(initialState.call());
                return source.materialize()
                        // do state transitions and emit notifications
                        // use flatMap to emit notification values
                        .flatMap(execute(transition, completion, state))
                        // flatten notifications to a stream which will enable
                        // early termination from the state machine if desired
                        .dematerialize();
            }
        });
    }

    private static <State, Out, In> Func1<Notification<In>, Observable<Notification<Out>>> execute(
            final Func3<? super State, ? super In, ? super Subscriber<Out>, ? extends State> transition,
            final Func2<? super State, ? super Subscriber<Out>, Boolean> completion,
            final AtomicReference<State> state) {

        return new Func1<Notification<In>, Observable<Notification<Out>>>() {

            @Override
            public Observable<Notification<Out>> call(final Notification<In> in) {

                return Observable.create(new OnSubscribe<Notification<Out>>() {

                    @Override
                    public void call(Subscriber<? super Notification<Out>> subscriber) {
                        Subscriber<Out> w = wrap(subscriber);
                        if (in.hasValue()) {
                            State nextState = transition.call(state.get(), in.getValue(), w);
                            state.set(nextState);
                            subscriber.onCompleted();
                        } else if (in.isOnCompleted()) {
                            if (completion.call(state.get(), w)) {
                                w.onCompleted();
                            }
                        } else {
                            w.onError(in.getThrowable());
                        }
                    }
                }).onBackpressureBuffer();
            }

        };
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
