package com.github.davidmoten.rx.testing;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

public class TestingHelperConcatTest extends TestCase {

    public static TestSuite suite() {
        return TestingHelper.function(CONCAT)
                // test empty
                .name("testConcatWithEmptyReturnsThree").fromEmpty().expect(1, 2, 3)
                // test error
                .name("testConcatErrorReturnsError").fromError().expectError()
                // test error after some emission
                .name("testConcatErrorAfterTwoEmissionsReturnsError").fromErrorAfter(5, 6)
                .expectError()
                // test non-empty count
                .name("testConcatWithTwoReturnsFive").from(5, 6).expect(5, 6, 1, 2, 3)
                // test single input
                .name("testConcatWithOneReturnsFour").from(5).expect(5, 1, 2, 3)
                // get test suites
                .testSuite(TestingHelperConcatTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }

    private static final Func1<Observable<Integer>, Observable<Integer>> CONCAT = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return o.concatWith(Observable.just(1, 2, 3));
        }
    };

}
