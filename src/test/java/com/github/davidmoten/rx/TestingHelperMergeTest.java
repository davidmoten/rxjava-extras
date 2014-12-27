package com.github.davidmoten.rx;

import static java.util.Arrays.asList;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

import com.github.davidmoten.rx.testing.TestingHelper;

public class TestingHelperMergeTest extends TestCase {

    private static final Func1<Observable<String>, Observable<String>> onBufferBackpressure = new Func1<Observable<String>, Observable<String>>() {
        @Override
        public Observable<String> call(Observable<String> o) {
            return o.mergeWith(Observable.from(asList("x", "y", "z")));
        }
    };

    public static TestSuite suite() {

        return TestingHelper.function(onBufferBackpressure)
                // test empty
                .name("testEmpty").fromEmpty().expect("x", "y", "z")
                // test non-empty count
                .name("testTwo").from("a", "b").expectAnyOrder("x", "y", "z", "a", "b")
                // test single input
                .name("testOne").from("a").expectAnyOrder("x", "y", "z", "a")
                // unsub before completion
                .name("testSomeUnsubscribeAfterOne").from("a", "b").unsubscribeAfter(1)
                .expectSize(1)
                // get test suites
                .testSuite(TestingHelperMergeTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }
}
