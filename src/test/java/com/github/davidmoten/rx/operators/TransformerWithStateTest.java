package com.github.davidmoten.rx.operators;

import static com.github.davidmoten.rx.Transformers.stateMachine;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.functions.Action2;
import rx.functions.Func3;

import com.github.davidmoten.rx.Transformers;

public class TransformerWithStateTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testBufferThreeUsingTranformerWithState() {
        final int bufferSize = 3;

        // In this case the type of State is List<Integer>
        List<Integer> initialState = Collections.<Integer> emptyList();
        List<List<Integer>> list = Observable
                .just(1, 2, 3, 4, 5)
                .compose(
                        Transformers.stateMachine(initialState,
                                TransformerWithStateTest.<Integer> transition(bufferSize),
                                TransformerWithStateTest.<Integer> bufferThreeCompletionAction()))
                .toList().toBlocking().single();
        assertEquals(asList(asList(1, 2, 3), asList(4, 5)), list);
    }

    private static <T> Func3<List<T>, T, Observer<List<T>>, List<T>> transition(final int bufferSize) {
        return new Func3<List<T>, T, Observer<List<T>>, List<T>>() {

            @Override
            public List<T> call(List<T> buffer, T value, Observer<List<T>> observer) {
                List<T> list = new ArrayList<T>(buffer);
                list.add(value);
                if (list.size() == bufferSize) {
                    observer.onNext(list);
                    return Collections.emptyList();
                } else {
                    return new ArrayList<T>(list);
                }
            }
        };

    }

    private static <T> Action2<List<T>, Observer<List<T>>> bufferThreeCompletionAction() {
        return new Action2<List<T>, Observer<List<T>>>() {

            @Override
            public void call(List<T> buffer, Observer<List<T>> observer) {
                if (buffer.size() > 0)
                    observer.onNext(buffer);
            }
        };
    }

    @Test
    public void testEmitLongCoolPeriodsUsingTransformerWithState() {
        // we are going to emit only those temperatures that are less than zero
        // and only when there are at least 4 values less than zero in a row
        int minLength = 4;
        int maxTemperature = 0;
        List<Integer> list = Observable
                .from(Arrays.asList(5, 3, 1, 0, -1, -2, -3, -3, -2, -1, 0, 1, 2, 3, 4, 0, -1, 2))
                .compose(
                        stateMachine(new State(false),
                                temperatureTransition(minLength, maxTemperature))).toList()
                .toBlocking().single();
        assertEquals(Arrays.asList(-1, -2, -3, -3, -2, -1), list);
    }

    private static class State {
        final List<Integer> temps;
        final boolean isOverMinLength;

        State(List<Integer> temps, boolean isOverMinLength) {
            this.temps = temps;
            this.isOverMinLength = isOverMinLength;
        }

        State(boolean isOverMinLength) {
            this(Collections.<Integer> emptyList(), isOverMinLength);
        }

    }

    private static Func3<State, Integer, Observer<Integer>, State> temperatureTransition(
            final int minLength, final float maxTemperatureCelsius) {

        return new Func3<State, Integer, Observer<Integer>, State>() {
            @Override
            public State call(State state, Integer temperatureCelsius, Observer<Integer> observer) {
                if (temperatureCelsius >= maxTemperatureCelsius) {
                    return new State(false);
                } else {
                    // is cool enough
                    if (state.isOverMinLength) {
                        observer.onNext(temperatureCelsius);
                        return state;
                    } else {
                        ArrayList<Integer> temps = new ArrayList<Integer>(state.temps);
                        temps.add(temperatureCelsius);
                        if (temps.size() == minLength) {
                            for (Integer t : temps) {
                                observer.onNext(t);
                            }
                            return new State(true);
                        } else
                            return new State(temps, false);
                    }
                }
            }
        };
    }
}
