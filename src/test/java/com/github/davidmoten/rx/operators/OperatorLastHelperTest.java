package com.github.davidmoten.rx.operators;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rx.Observable;

public class OperatorLastHelperTest {

    @Test
    public void testLastOfTenReturnsLast() {
        assertEquals(10, (int) Observable.range(1, 10).lift(OperatorLast.<Integer> create())
                .toBlocking().single());
    }

    @Test(expected = RuntimeException.class)
    public void testLastOfEmptyThrowsError() {
        Observable.empty().lift(OperatorLast.create()).toBlocking().single();
    }

    @Test
    public void testLastOfOneReturnsLast() {
        assertEquals(1, (int) Observable.range(1, 1).lift(OperatorLast.<Integer> create())
                .toBlocking().single());
    }

}
