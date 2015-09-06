package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static rx.Observable.just;

import org.junit.Test;

import com.github.davidmoten.rx.Strings;

import rx.Observable;

public class StringsTest {

    @Test
    public void testTrim() {
        assertEquals("trimmed", Strings.trim().call("  \ttrimmed\r\n   "));
    }

    @Test
    public void testTrimOnNullInputReturnsNull() {
        assertNull(Strings.trim().call(null));
    }

    @Test
    public void testJoinTwo() {
        assertEquals("a,b", Strings.join(just("a", "b"), ",").toBlocking().single());
    }

    @Test
    public void testJoinOne() {
        assertEquals("a", Strings.join(just("a")).toBlocking().single());
    }

    @Test
    public void testJoinNone() {
        assertEquals(0,
                Strings.join(Observable.<String> empty()).toList().toBlocking().single().size());
    }

}
