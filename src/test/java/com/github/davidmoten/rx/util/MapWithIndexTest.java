package com.github.davidmoten.rx.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import rx.Observable;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.util.MapWithIndex;
import com.github.davidmoten.rx.util.MapWithIndex.Indexed;

public class MapWithIndexTest {

    @Test
    public void testEmpty() {
        assertTrue(Observable.empty().compose(Transformers.mapWithIndex()).isEmpty().toBlocking()
                .single());
    }

    @Test
    public void testOne() {
        List<Indexed<String>> list = Observable.just("a")
                .compose(Transformers.<String> mapWithIndex()).toList().toBlocking().single();
        assertEquals(1, list.size());
        assertEquals(0, list.get(0).index());
        assertEquals("a", list.get(0).value());
    }

    @Test
    public void testTwo() {
        List<Indexed<String>> list = Observable.just("a", "b")
                .compose(Transformers.<String> mapWithIndex()).toList().toBlocking().single();
        assertEquals(2, list.size());
        assertEquals(0, list.get(0).index());
        assertEquals("a", list.get(0).value());
        assertEquals(1, list.get(1).index());
        assertEquals("b", list.get(1).value());
    }

    @Test
    public void testTwoAgain() {
        List<Indexed<String>> list = Observable.just("a", "b")
                .compose(MapWithIndex.<String> instance()).toList().toBlocking().single();
        assertEquals(2, list.size());
        assertEquals(0, list.get(0).index());
        assertEquals("a", list.get(0).value());
        assertEquals(1, list.get(1).index());
        assertEquals("b", list.get(1).value());
    }

}
