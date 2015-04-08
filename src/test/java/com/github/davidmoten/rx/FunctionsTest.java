package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rx.Observable;

import com.github.davidmoten.rx.Functions.Statistics;
import com.github.davidmoten.util.TestingUtil;

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
    public void testNot() {
        assertEquals(false, (boolean) Functions.not(Functions.alwaysTrue()).call(123));
    }

    @Test
    public void testConstructorIsPrivate() {
        TestingUtil.callConstructorAndCheckIsPrivate(Functions.class);
    }

    @Test
    public void testStatistics() {
        Observable<Integer> nums = Observable.just(1, 4, 10, 20);
        Statistics s = nums.compose(Functions.collectStats()).last().toBlocking().single();
        assertEquals(4, s.count());
        assertEquals(35.0, s.sum(), 0.0001);
        assertEquals(8.75, s.mean(), 0.00001);
        assertEquals(7.258615570478987, s.sd(), 0.00001);
    }
}
