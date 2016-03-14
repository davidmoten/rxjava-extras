package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;

public class ActionsTest {

    @Test
    public void testAtomicInteger() {
        AtomicInteger a = new AtomicInteger();
        Actions.setAtomic(a).call(1);
        assertEquals(1, a.get());
    }

    @Test
    public void testAtomicLong() {
        AtomicLong a = new AtomicLong();
        Actions.setAtomic(a).call(1L);
        assertEquals(1, a.get());
    }

    @Test
    public void testAtomicBoolean() {
        AtomicBoolean a = new AtomicBoolean();
        Actions.setAtomic(a).call(true);
        assertTrue(a.get());
    }

    @Test
    public void testAtomicReference() {
        AtomicReference<Integer> a = new AtomicReference<Integer>();
        Actions.setAtomic(a).call(1);
        assertEquals(1, (int) a.get());
    }
    
    @Test
    public void isUtilClass() {
        Asserts.assertIsUtilityClass(Actions.class);
    }

}
