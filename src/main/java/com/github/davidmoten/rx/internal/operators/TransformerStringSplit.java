package com.github.davidmoten.rx.internal.operators;

import java.util.regex.Pattern;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.util.Preconditions;

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
                if (!observer.isUnsubscribed())
                    observer.onCompleted();
                return true;
            }
        };
        return com.github.davidmoten.rx.Transformers.stateMachine(initialState, transition, completion);
    }

	public static <T> Transformer<String, String> split(final int maxItemLength, final String pattern, final Pattern compiledPattern, final int maxPatternLength) {
	    Preconditions.checkArgument(maxItemLength >= 0,"maxItemLength cannot be negative");
	    Preconditions.checkArgument(pattern != null ^ compiledPattern != null, "exactly one of pattern and compiledPattern can be set");
	    Preconditions.checkArgument(maxPatternLength > 0, "maxPatternLength must be positive");
	    
		Func0<State> initialState = Functions.constant0(new State(null, false));
		Func3<State, String, Subscriber<String>, State> transition = new Func3<State, String, Subscriber<String>, State>() {

			@Override
            public State call(State state, String s, Subscriber<String> observer) {

                // we make the assumption that each new s has been sensibly limited in size so
                // we quite happily concatenate state.leftOver with s

                // prepend leftover to the string before attempting split
                if (state.leftOver != null) {
                    s = state.leftOver + s;
                }

                String[] parts;
                if (compiledPattern != null) {
                    parts = compiledPattern.split(s, -1);
                } else {
                    parts = s.split(pattern, -1);
                }
                // parts length will be 1 if pattern not matched

                final int firstPartIndex;
                if (state.atMaxLength) {
                    // we have already emitted the first maxLength characters of the current item
                    // and we are continuing to look for the next delimiter (pattern) match 
                    if (parts.length == 1) {
                        // no match to the pattern
                        //
                        // just retain the last maxPatternLength characters because we are ignoring
                        // characters before the pattern occurs again
                        s = tail(s, maxPatternLength);
                        return new State(s, true);
                    } else {
                        // pattern found but we skip emitting the first token (because the trimmed
                        // version of the token has already been emitted).
                        firstPartIndex = 1;
                    }
                } else {
                    firstPartIndex = 0;
                }

                // can emit all parts except the last part (and possible the first part if we
                // were already at maxLength and emitted the characters at the start) because it
                // hasn't been terminated by the pattern/end-of-stream yet
                for (int i = firstPartIndex; i < parts.length - 1; i++) {
                    if (observer.isUnsubscribed()) {
                        // won't be used so can return null
                        return null;
                    }
                    observer.onNext(limit(parts[i], maxItemLength));
                }

                // we have to assign the last part as leftOver because we
                // don't know if it has been terminated yet
                String last = parts[parts.length - 1];
                if (maxItemLength > 0 && last.length() > maxItemLength + maxPatternLength) {
                    if (!state.atMaxLength) {
                        if (observer.isUnsubscribed()) {
                            // won't be used so can return null
                            return null;
                        }
                        observer.onNext(limit(last, maxItemLength));
                    }
                    return new State(tail(last, maxPatternLength), true);
                } else {
                    return new State(parts[parts.length - 1], false);
                }
            }

		};

		Func2<State, Subscriber<String>, Boolean> completion = new Func2<State, Subscriber<String>, Boolean>() {

			@Override
			public Boolean call(State state, Subscriber<String> observer) {
				if (!state.atMaxLength && state.leftOver != null && !observer.isUnsubscribed()) {
					observer.onNext(limit(state.leftOver, maxItemLength));
				}
				if (!observer.isUnsubscribed()) {
					observer.onCompleted();
				}
				return true;
			}
		};
		return com.github.davidmoten.rx.Transformers.stateMachine(initialState, transition, completion);
	}

	private static String tail(String s, int maxPatternLength) {
        return s.substring(Math.max(0, s.length() - maxPatternLength), s.length());
    }
	
	private static String limit(String s, int maxLength) {
	    if (s.length() <= maxLength || maxLength == 0) {
	        return s;
	    } else {
	        return s.substring(0, maxLength);
	    }
	}
	
    private static final class State {

        // accumulated characters while looking for delimiter pattern
        // if atMaxLength is false
        final String leftOver;

        // if true then we have already emitted the item at maxLength characters and
        // leftOver will contain a sliding window of up to maxPatternLength characters
        final boolean atMaxLength;

        State(String leftOver, boolean atMaxLength) {
            this.leftOver = leftOver;
            this.atMaxLength = atMaxLength;
        }
    }

}
