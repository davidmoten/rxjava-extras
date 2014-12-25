package com.github.davidmoten.rx;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

import com.github.davidmoten.rx.testing.TestingHelper;

public class TestingHelperOnBackpressureTest extends TestCase {

    private static final Func1<Observable<String>, Observable<String>> onBufferBackpressure = new Func1<Observable<String>, Observable<String>>() {
        @Override
        public Observable<String> call(Observable<String> o) {
            return o.onBackpressureBuffer();
        }
    };

    public static TestSuite suite() {

        return TestingHelper.function(onBufferBackpressure)
                // test empty
                .name("testEmpty").fromEmpty().expectEmpty()
                // test non-empty count
                .name("testTwo").from("a", "b").expect("a", "b")
                // test single input
                .name("testOne").from("a").expect("a")
                // unsub before completion
                .name("testTwoUnsubscribeAfterOne").from("a", "b", "c", "d").unsubscribeAfter(1)
                .expect("a")
                // get test suites
                .testSuite(TestingHelperOnBackpressureTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }
}
