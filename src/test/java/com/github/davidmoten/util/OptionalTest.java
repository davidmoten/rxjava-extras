package com.github.davidmoten.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.github.davidmoten.util.Optional.NotPresentException;

public class OptionalTest {

    @Test
    public void testOf() {
        assertEquals(1L, (long) Optional.of(1L).get());
    }

    @Test(expected = NotPresentException.class)
    public void testAbsentGetThrowsException() {
        Optional.<Long> absent().get();
    }

    @Test
    public void testOfNullIsOk() {
        assertNull(Optional.of(null).get());
    }

    @Test
    public void testOfIsPresent() {
        assertTrue(Optional.of(1).isPresent());
    }

    @Test
    public void testAbsentIsNotPresent() {
        assertFalse(Optional.absent().isPresent());
    }

    @Test
    public void testFromNullableFromNullReturnsAbsent() {
        assertFalse(Optional.fromNullable(null).isPresent());
    }

    @Test
    public void testFromNullableFromNonNullReturnsPresent() {
        assertTrue(Optional.fromNullable(1).isPresent());
    }

    @Test
    public void testOrWhenPresent() {
        assertEquals(1L, (long) Optional.of(1).or(2));
    }

    @Test
    public void testOrWhenNotPresent() {
        assertEquals(2L, (long) Optional.<Long> absent().or(2L));
    }

    @Test
    public void testAbsentObservable() {
        assertTrue(Optional.absent().toObservable().isEmpty().toBlocking().single());
    }

    @Test
    public void testPresentObservable() {
        assertEquals(Arrays.asList(1),
                Optional.of(1).toObservable().toList().toBlocking().single());
    }

}
