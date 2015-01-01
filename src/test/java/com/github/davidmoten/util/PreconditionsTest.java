package com.github.davidmoten.util;

import org.junit.Test;

public class PreconditionsTest {

    @Test(expected = NullPointerException.class)
    public void testNullThrowsException() {
        Preconditions.checkNotNull(null);
    }

    @Test
    public void testNotNullDoesNotThrowException() {
        Preconditions.checkNotNull(new Object());
    }
}
