package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FunctionsTest {

    @Test
    public void testIdentity() {
        assertEquals(123, (int) Functions.<Integer> identity().call(123));
    }

    @Test
    public void testAlwaysTrue() {
        assertTrue(Functions.<Integer> alwaysTrue().call(123));
    }

    @Test
    public void testAlwaysFalse() {
        assertFalse(Functions.<Integer> alwaysFalse().call(123));
    }

    @Test
    public void testConstant() {
        assertEquals(123, (int) Functions.constant(123).call(1));
    }

    @Test
    public void testConstructorIsPrivate() {
        TestingUtil.callConstructorAndCheckIsPrivate(Functions.class);
    }
}

