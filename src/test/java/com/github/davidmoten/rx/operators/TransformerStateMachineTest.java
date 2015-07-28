package com.github.davidmoten.rx.operators;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.observers.TestSubscriber;

import com.github.davidmoten.rx.Transformers;

public class TransformerStateMachineTest {
    @SuppressWarnings("unchecked")
    @Test
    public void testToListUntilChanged() {
        Observable<Integer> o = Observable.just(1, 1, 1, 2, 2, 3);
        List<List<Integer>> lists = o.compose(Transformers.<Integer> toListWhileEqual())
        // get as list
                .toList().toBlocking().single();
        assertEquals(asList(asList(1, 1, 1), asList(2, 2), asList(3)), lists);
    }

    @Test
    public void testToListUntilChangedMultipleAtEnd() {
        Observable<Integer> o = Observable.just(1, 1, 1, 2, 2, 3, 3);
        List<List<Integer>> lists = o.compose(Transformers.<Integer> toListWhileEqual())
        // get as list
                .toList().toBlocking().single();
        assertEquals(asList(asList(1, 1, 1), asList(2, 2), asList(3, 3)), lists);
    }

    @Test
    public void testToListUntilChangedWithEmpty() {
        Observable<Integer> o = Observable.empty();
        List<List<Integer>> lists = o.compose(Transformers.<Integer> toListWhileEqual())
        // get as list
                .toList().toBlocking().single();
        assertTrue(lists.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToListUntilChangedWithNoChange() {
        Observable<Integer> o = Observable.just(1, 1, 1);
        List<List<Integer>> lists = o.compose(Transformers.<Integer> toListWhileEqual())
        // get as list
                .toList().toBlocking().single();
        assertEquals(asList(asList(1, 1, 1)), lists);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testToListUntilChangedWithOnlyChange() {
        Observable<Integer> o = Observable.just(1, 2, 3);
        List<List<Integer>> lists = o.compose(Transformers.<Integer> toListWhileEqual())
        // get as list
                .toList().toBlocking().single();
        assertEquals(asList(asList(1), asList(2), asList(3)), lists);
    }

    @Test
    public void testToListUntilChangedWithError() {
        Exception ex = new Exception("boo");
        Observable<Integer> o = Observable.error(ex);
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        o.compose(Transformers.<Integer> toListWhileEqual()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertError(ex);
        ts.assertUnsubscribed();
    }

}
