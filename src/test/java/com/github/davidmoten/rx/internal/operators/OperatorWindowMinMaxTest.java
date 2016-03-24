package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.github.davidmoten.rx.Transformers;

import rx.Observable;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OperatorWindowMinMaxTest {

    @Test
    public void testEmpty() {
        boolean empty = Observable.<Integer> empty().compose(Transformers.<Integer> windowMin(5))
                .isEmpty().toBlocking().single();
        assertTrue(empty);
    }

    @Test
    public void testIncreasing() {
        List<Integer> list = Observable.just(1, 2, 3, 4)
                .compose(Transformers.<Integer> windowMin(2)).toList().toBlocking().single();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testDecreasing() {
        List<Integer> list = Observable.just(4, 3, 2, 1)
                .compose(Transformers.<Integer> windowMin(2)).toList().toBlocking().single();
        assertEquals(Arrays.asList(3, 2, 1), list);
    }

    @Test
    public void testWindowSizeBiggerThanAvailableProducesEmptyList() {
        List<Integer> list = Observable.just(4, 3, 2, 1)
                .compose(Transformers.<Integer> windowMin(10)).toList().toBlocking().single();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testWindowMax() {
        List<Integer> list = Observable.just(4, 3, 2, 1)
                .compose(Transformers.<Integer> windowMax(2)).toList().toBlocking().single();
        assertEquals(Arrays.asList(4, 3, 2), list);
    }

}
