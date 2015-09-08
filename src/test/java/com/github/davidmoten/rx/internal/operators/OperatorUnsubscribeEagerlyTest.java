package com.github.davidmoten.rx.internal.operators;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

import com.github.davidmoten.rx.internal.operators.OperatorUnsubscribeEagerly;
import com.github.davidmoten.rx.testing.TestingHelper;

public class OperatorUnsubscribeEagerlyTest extends TestCase {

    public static TestSuite suite() {
        return TestingHelper.function(function())
                // test empty
                .name("testUnsubEagerOfEmptyReturnsEmpty").fromEmpty().expectEmpty()
                // test error
                .name("testUnsubEagerErrorReturnsError").fromError().expectError()
                // test error after some emission
                .name("testUnsubEagerReturnsUnchanged").from(5, 6).expect(5, 6)
                // get test suites
                .testSuite(OperatorUnsubscribeEagerlyTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }

    private static final <T> Func1<Observable<T>, Observable<T>> function() {
        return new Func1<Observable<T>, Observable<T>>() {

            @Override
            public Observable<T> call(Observable<T> o) {
                return o.lift(OperatorUnsubscribeEagerly.<T> instance());
            }
        };
    }
}
