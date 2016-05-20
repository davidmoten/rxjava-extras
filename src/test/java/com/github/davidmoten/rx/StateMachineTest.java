package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.davidmoten.rx.StateMachine.Completion;
import com.github.davidmoten.rx.StateMachine.Transition;
import com.github.davidmoten.rx.util.BackpressureStrategy;

import rx.Observable;
import rx.Observable.Transformer;
import rx.Subscriber;

public class StateMachineTest {

	@Test
	public void testBuilder() {
		 Transformer<Integer, String> collectIntoStringsOfMinLength3 = Transformers.stateMachine() //
				.initialState("") //
				.transition(new Transition<String, Integer, String>() {
					@Override
					public String call(String state, Integer value, Subscriber<String> subscriber) {
						String state2 = state + value;
						if (state2.length() >= 3) {
							subscriber.onNext(state2.substring(0, 3));
							return state2.substring(3);
						} else {
							return state2;
						}
					}
					
				}) //
				.completion(new Completion<String, String>() {
					@Override
					public Boolean call(String state, Subscriber<String> subscriber) {
						subscriber.onNext(state);
						return true;
					}
				}) //
				.backpressureStrategy(BackpressureStrategy.BUFFER) //
				.build();
		
		List<String> list = Observable.range(1, 13).compose(collectIntoStringsOfMinLength3).toList().toBlocking().single();
		assertEquals(Arrays.asList("123", "456", "789", "101", "112", "13"), list);

	}

}
