package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.github.davidmoten.rx.Strings;

public class StringsTest {

    @Test
    public void testTrim() {
        assertEquals("trimmed", Strings.trim().call("  \ttrimmed\r\n   "));
    }

    @Test
    public void testTrimOnNullInputReturnsNull() {
        assertNull(Strings.trim().call(null));
    }

}
