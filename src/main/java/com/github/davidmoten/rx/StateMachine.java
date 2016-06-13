package com.github.davidmoten.rx;

import com.github.davidmoten.rx.util.BackpressureStrategy;

import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.functions.Func3;

public final class StateMachine {

    private StateMachine() {
        // prevent instantiation
    }

    public static interface Transition<State, In, Out>
            extends Func3<State, In, Subscriber<Out>, State> {

        // override so IDEs have better suggestions for parameters
        @Override
        public State call(State state, In value, Subscriber<Out> subscriber);

    }

    public static interface Completion<State, Out> extends Func2<State, Subscriber<Out>, Boolean> {

        // override so IDEs have better suggestions for parameters
        @Override
        public Boolean call(State state, Subscriber<Out> subscriber);

    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Builder() {
            // prevent instantiation from other packages
        }

        public <State> Builder2<State> initialStateFactory(Func0<State> initialState) {
            return new Builder2<State>(initialState);
        }

        public <State> Builder2<State> initialState(final State initialState) {
            return new Builder2<State>(Functions.constant0(initialState));
        }

    }

    public static final class Builder2<State> {

        private final Func0<State> initialState;

        private Builder2(Func0<State> initialState) {
            this.initialState = initialState;
        }

        public <In, Out> Builder3<State, In, Out> transition(
                Transition<State, In, Out> transition) {
            return new Builder3<State, In, Out>(initialState, transition);
        }

    }

    public static final class Builder3<State, In, Out> {

        private final Func0<State> initialState;
        private final Transition<State, In, Out> transition;
        private Completion<State, Out> completion = CompletionAlwaysTrueHolder.instance();
        private BackpressureStrategy backpressureStrategy = BackpressureStrategy.BUFFER;
        private int initialRequest = Transformers.DEFAULT_INITIAL_BATCH;

        private Builder3(Func0<State> initialState, Transition<State, In, Out> transition) {
            this.initialState = initialState;
            this.transition = transition;
        }

        public Builder3<State, In, Out> completion(Completion<State, Out> completion) {
            this.completion = completion;
            return this;
        }

        public Builder3<State, In, Out> backpressureStrategy(
                BackpressureStrategy backpressureStrategy) {
            this.backpressureStrategy = backpressureStrategy;
            return this;
        }

        public Builder3<State, In, Out> initialRequest(int value) {
            this.initialRequest = value;
            return this;
        }

        public Transformer<In, Out> build() {
            return Transformers.stateMachine(initialState, transition, completion,
                    backpressureStrategy, initialRequest);
        }

    }

    private static final class CompletionAlwaysTrueHolder {

        private static final Completion<Object, Object> INSTANCE = new Completion<Object, Object>() {
            @Override
            public Boolean call(Object t1, Subscriber<Object> t2) {
                return true;
            }
        };

        @SuppressWarnings("unchecked")
        static <State, Out> Completion<State, Out> instance() {
            return (Completion<State, Out>) INSTANCE;
        }
    }
}
