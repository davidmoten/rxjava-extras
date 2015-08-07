package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.google.common.util.concurrent.AtomicDouble;

public class ActionsTest {

    @Test
    public void testAtomicInteger() {
        AtomicInteger a = new AtomicInteger();
        Actions.setAtomic(a).call(1);
        assertEquals(1, a.get());
    }

    @Test
    public void testAtomicDouble() {
        AtomicDouble a = new AtomicDouble();
        Actions.setAtomic(a).call(1.0);
        assertEquals(1.0, a.get(), 0.0001);
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

}
