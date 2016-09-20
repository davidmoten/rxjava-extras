package com.github.davidmoten.rx.testing;

import org.junit.Test;

import rx.Observable;

public class TestingHelperTest {
    
    @Test
    public void test() {
        Observable.range(1, 1000)
        .to(TestingHelper.<Integer>testWithRequest(2))
        .assertValues(1, 2)
        .assertNoTerminalEvent();
    }

}
