package com.github.davidmoten.rx.operators;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import com.github.davidmoten.rx.testing.TestingHelper;

public class OperatorReduceHelperTest extends TestCase {

    public static TestSuite suite() {
        return TestingHelper.function(count).waitForUnsubscribe(100, TimeUnit.MILLISECONDS)
                .waitForTerminalEvent(10, TimeUnit.SECONDS)
                .waitForMoreTerminalEvents(100, TimeUnit.MILLISECONDS)
                // test empty
                .name("testReduceOfEmptyReturnsInitialValue").fromEmpty().expect(0)
                // test error
                .name("testReduceErrorReturnsError").fromError().expectError()
                // test error after items
                .name("testReduceErrorAfter2ReturnsError").fromErrorAfter(1, 2).expectError()
                // test non-empty count
                .name("testReduceTwoReturnsTwo").from(3, 5).expect(2)
                // unsub before completion
                .name("testTwoUnsubscribedAfterOneReturnsOneItemOnly").from(3, 5)
                .unsubscribeAfter(1).expect(2)
                // get test suites
                .testSuite(OperatorReduceHelperTest.class);
    }

    @Test
    public void testToKeepEclipseHappy() {
        // just here to fool eclipse
    }

    @Test
    public void testSumWithNoInitialValue() {
        Func2<Integer, Integer, Integer> sum = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer a, Integer b) {
                return a + b;
            }
        };
        int result = Observable.range(1, 4).lift(OperatorReduce.create(sum)).toBlocking().single();
        assertEquals(10, result);
    }

    @Test
    public void testSumWithEmpty() {
        Func2<Integer, Integer, Integer> sum = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer a, Integer b) {
                return a + b;
            }
        };
        int result = Observable.range(1, 4).lift(OperatorReduce.create(sum)).toBlocking().single();
        assertEquals(10, result);
    }

    private static final Func1<Observable<Integer>, Observable<Integer>> count = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return o.lift(OperatorReduce.create(0, new Func2<Integer, Integer, Integer>() {

                @Override
                public Integer call(Integer n, Integer o) {
                    return n + 1;
                }
            }));
        }
    };

}
