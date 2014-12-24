package com.github.davidmoten.rx;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

public class OperatorCountTest extends TestCase {

    private static final Func1<Observable<String>, Observable<Integer>> COUNT = new Func1<Observable<String>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<String> o) {
            return o.count();
        }
    };

    public static TestSuite suite() {

        return TestingHelper.function(COUNT)
        // test empty
                .name("testEmpty").fromEmpty().expect(0)
                // test non-empty count
                .name("testTwo").from("a", "b").expect(2)
                // test single input
                .name("testOne").from("a").expect(1)
                // unsub before completion
                .name("testTwoUnsubscribeAfterOne").from("a", "b").unsubscribeAfter(1).expect(2)
                // get test suite
                .testSuite();
    }
}
