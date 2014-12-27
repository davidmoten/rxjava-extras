package com.github.davidmoten.rx;

import static java.util.Arrays.asList;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

import com.github.davidmoten.rx.testing.TestingHelper;

public class TestingHelperMergeTest extends TestCase {

    private static final Func1<Observable<Integer>, Observable<Integer>> merge = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return o.mergeWith(Observable.from(asList(7, 8, 9)));
        }
    };

    public static TestSuite suite() {

        return TestingHelper.function(merge)
        // test empty
                .name("testEmpty").fromEmpty().expect(7, 8, 9)
                // test non-empty count
                .name("testTwo").from(1, 2).expectAnyOrder(1, 7, 8, 9, 2)
                // test single input
                .name("testOne").from(1).expectAnyOrder(7, 1, 8, 9)
                // unsub before completion
                .name("testSomeUnsubscribeAfterOne").from(1, 2).unsubscribeAfter(1).expectSize(1)
                // get test suites
                .testSuite(TestingHelperMergeTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }
}
