package com.github.davidmoten.util;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
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

    @Test
    public void testCheckArgumentThrowsIAE() {
        try {
            Preconditions.checkArgument(false, "hi");
            Assert.fail();
        } catch (IllegalArgumentException e) {
            assertEquals("hi", e.getMessage());
        }
    }

    @Test
    public void testCheckArgumentDoesNotThrowIAE() {
        Preconditions.checkArgument(true, "hi");
    }

}
