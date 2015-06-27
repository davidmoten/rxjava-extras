package com.github.davidmoten.rx.operators;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.functions.Func0;
import rx.functions.Func4;

import com.github.davidmoten.rx.Transformers;

public class TransformerWithStateTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testBufferThree() {
        final int bufferSize = 3;
        List<List<Integer>> list = Observable
                .just(1, 2, 3, 4, 5)
                .compose(
                        Transformers.withState(TransformerWithStateTest.<Integer> initialState(),
                                TransformerWithStateTest.<Integer> transition(bufferSize)))
                .toList().toBlocking().single();
        System.out.println(list);
        assertEquals(asList(asList(1, 2, 3), asList(4, 5)), list);
    }

    private static <T> Func0<List<T>> initialState() {
        return new Func0<List<T>>() {

            @Override
            public List<T> call() {
                return Collections.emptyList();
            }
        };
    }

    private static <T> Func4<List<T>, T, Boolean, Observer<List<T>>, List<T>> transition(
            final int bufferSize) {
        return new Func4<List<T>, T, Boolean, Observer<List<T>>, List<T>>() {

            @Override
            public List<T> call(List<T> buffer, T t, Boolean completed, Observer<List<T>> observer) {
                List<T> list = new ArrayList<T>(buffer);
                if (!completed) {
                    list.add(t);
                    if (list.size() == bufferSize) {
                        observer.onNext(list);
                        return Collections.emptyList();
                    } else {
                        return new ArrayList<T>(list);
                    }
                } else {
                    if (buffer.size() > 0)
                        observer.onNext(buffer);
                    return null;
                }

            }
        };

    }

}
