package com.github.davidmoten.rx.operators;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
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
            public List<T> call(List<T> buffer, T value, Boolean completed, Observer<List<T>> observer) {
                List<T> list = new ArrayList<T>(buffer);
                if (completed) {
                    if (buffer.size() > 0)
                        observer.onNext(buffer);
                    // return is irrelevant
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

}
