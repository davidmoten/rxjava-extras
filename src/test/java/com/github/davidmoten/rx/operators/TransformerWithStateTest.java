package com.github.davidmoten.rx.operators;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.functions.Func4;

import com.github.davidmoten.rx.Transformers;

public class TransformerWithStateTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testBufferThree() {
        final int bufferSize = 3;

        // In this case the type of State is List<Integer>
        List<Integer> initialState = Collections.<Integer> emptyList();
        List<List<Integer>> list = Observable
                .just(1, 2, 3, 4, 5)
                .compose(
                        Transformers.withState(initialState,
                                TransformerWithStateTest.<Integer> transition(bufferSize)))
                .toList().toBlocking().single();
        System.out.println(list);
        assertEquals(asList(asList(1, 2, 3), asList(4, 5)), list);
    }

    private static <T> Func4<List<T>, T, Boolean, Observer<List<T>>, List<T>> transition(
            final int bufferSize) {
        return new Func4<List<T>, T, Boolean, Observer<List<T>>, List<T>>() {

            @Override
            public List<T> call(List<T> buffer, T value, Boolean completed,
                    Observer<List<T>> observer) {
                List<T> list = new ArrayList<T>(buffer);
                if (completed) {
                    if (buffer.size() > 0)
                        observer.onNext(buffer);
                    // return is irrelevant when complete
                    return null;
                } else {
                    list.add(value);
                    if (list.size() == bufferSize) {
                        observer.onNext(list);
                        return Collections.emptyList();
                    } else {
                        return new ArrayList<T>(list);
                    }
                }

            }
        };

    }

    private static class Temperature {
        final float temperatureCelcius;
        final long time;

        Temperature(float temperatureCelcius, long time) {
            this.temperatureCelcius = temperatureCelcius;
            this.time = time;
        }

    }

    static Temperature temperature(float t, long time) {
        return new Temperature(t, time);
    }

    @Test
    public void testEmitLongCoolPeriods() {
        // we are going to emit only those temperatures that are less than zero
        // and when there are at least 4 values less than zero in a row
        List<Integer> list = Observable
                .from(Arrays.asList(5, 3, 1, 0, -1, -2, -3, -3, -2, -1, 0, 1, 2, 3, 4, 0, -1, 2))
                .compose(Transformers.withState(new State(false), temperatureTransition(4, 0)))
                .toList().toBlocking().single();
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

    private static Func4<State, Integer, Boolean, Observer<Integer>, State> temperatureTransition(
            final int minLength, final float maxTemperatureCelsius) {

        return new Func4<State, Integer, Boolean, Observer<Integer>, State>() {
            @Override
            public State call(State state, Integer temperatureCelsius, Boolean completed,
                    Observer<Integer> observer) {
                if (completed) {
                    // return is irrelevant when complete
                    return null;
                } else if (temperatureCelsius >= maxTemperatureCelsius) {
                    return new State(false);
                } else {
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
