package com.github.davidmoten.rx;

import rx.Observable.Transformer;

import com.github.davidmoten.rx.util.BackpressureStrategy;

import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.functions.Func3;

public final class StateMachine {
	
	private StateMachine() {
		// prevent instantiation
	}

    public static interface Transition<State, In, Out> extends Func3<State, In, Subscriber<Out>, State> {
    	
    	//override so IDEs have better suggestions for parameters
    	@Override
    	public State call(State state, In value, Subscriber<Out> subscriber);
    	
    }
    
    public static interface Completion<State, Out> extends Func2<State, Subscriber<Out>, Boolean> {

    	//override so IDEs have better suggestions for parameters
    	@Override
    	public Boolean call(State state, Subscriber<Out> subscriber);
    	
    }
    
    public static Builder builder() {
    	return new Builder();
    }
    
    public static class Builder{
    	
    	Builder() {
    		
    	}
    	
    	public <State> Builder2<State> initialStateCreator(Func0<State> initialState) {
			return new Builder2<State>(initialState);
    	}
    	
    	public <State> Builder2<State> initialState(final State state) {
    		return new Builder2<State>(Functions.constant0(state));
    	}
    	
    }
    
    public static class Builder2<State> {

		private Func0<State> initialState;

		Builder2(Func0<State> initialState) {
			this.initialState = initialState;
		}

		public <In,Out> Builder3<State,In,Out> transition(Transition<State,In,Out> transition) {
			return new Builder3<State,In,Out>(initialState, transition);
		}
		
    }
    
    public static class Builder3<State,In,Out> {

		private final Func0<State> initialState;
		private final Transition<State, In, Out> transition;
		private Completion<State, Out> completion = CompletionAlwaysTrueHolder.instance();
		private BackpressureStrategy backpressureStrategy = BackpressureStrategy.BUFFER;

		public Builder3(Func0<State> initialState, Transition<State, In, Out> transition) {
			this.initialState = initialState;
			this.transition = transition;
		}
		
		public Builder3<State,In,Out> completion(Completion<State,Out> completion) {
			this.completion = completion;
			return this;
		}
		
		public Builder3<State,In,Out> backpressureStrategy(BackpressureStrategy backpressureStrategy) {
			this.backpressureStrategy = backpressureStrategy;
			return this;
		}
		
		public Transformer<In,Out> build() {
			return Transformers.stateMachine(initialState, transition, completion, backpressureStrategy);
		}
		
    }
    
    private static final class CompletionAlwaysTrueHolder {
    	
    	private static final Completion<Object,Object> INSTANCE = new Completion<Object,Object> () {
			@Override
			public Boolean call(Object t1, Subscriber<Object> t2) {
				return true;
			}};
			
	    @SuppressWarnings("unchecked")
		static <State,Out> Completion<State,Out> instance() {
	    	return (Completion<State, Out>) INSTANCE;
	    }
    }
}
