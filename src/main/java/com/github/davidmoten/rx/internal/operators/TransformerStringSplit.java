package com.github.davidmoten.rx.internal.operators;

import java.util.regex.Pattern;

import com.github.davidmoten.rx.Functions;

import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.functions.Func3;

public final class TransformerStringSplit {

	public static <T> Transformer<String, String> split(final String pattern, final Pattern compiledPattern) {
		Func0<String> initialState = Functions.constant0(null);
		Func3<String, String, Subscriber<String>, String> transition = new Func3<String, String, Subscriber<String>, String>() {

			@Override
			public String call(String leftOver, String s, Subscriber<String> observer) {
                // prepend leftover to the string before splitting
			    if (leftOver != null) {
			        s = leftOver + s;
			    }
			    
				String[] parts;
				if (compiledPattern != null) {
					parts = compiledPattern.split(s, -1);
				} else {
					parts = s.split(pattern, -1);
				}

				// can emit all parts except the last part because it hasn't
				// been terminated by the pattern/end-of-stream yet
				for (int i = 0; i < parts.length - 1; i++) {
					if (observer.isUnsubscribed()) {
						// won't be used so can return null
						return null;
					}
					observer.onNext(parts[i]);
				}

				// we have to assign the last part as leftOver because we
				// don't know if it has been terminated yet
				return parts[parts.length - 1];
			}
		};

		Func2<String, Subscriber<String>, Boolean> completion = new Func2<String, Subscriber<String>, Boolean>() {

			@Override
			public Boolean call(String leftOver, Subscriber<String> observer) {
				if (leftOver != null && !observer.isUnsubscribed())
					observer.onNext(leftOver);
				// TODO is this check needed?
				if (!observer.isUnsubscribed())
					observer.onCompleted();
				return true;
			}
		};
		return com.github.davidmoten.rx.Transformers.stateMachine(initialState, transition, completion);
	}

}
