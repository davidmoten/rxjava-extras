package com.github.davidmoten.rx.operators;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import com.github.davidmoten.rx.testing.TestingHelper;

public class OperatorReduceTest extends TestCase {


    public static TestSuite suite() {
        return TestingHelper
                .function(count)
                .waitForUnsubscribe(100, TimeUnit.MILLISECONDS)
                .waitForTerminalEvent(10, TimeUnit.SECONDS)
                .waitForMoreTerminalEvents(100, TimeUnit.MILLISECONDS)
                // test empty
                .name("testReduceOfEmptyReturnsInitialValue")
                .fromEmpty()
                .expect(0)
                // test error
                .name("testReduceErrorReturnsError")
                .fromError()
                .expectError()
                // test error after items
                .name("testReduceErrorAfter2ReturnsError")
                .fromErrorAfter(1, 2)
                .expectError()
                // test non-empty count
                .name("testReduceTwoReturnsTwo")
                .from(3,5)
                .expect(2)
                // unsub before completion
                .name("testTwoWithOtherUnsubscribedAfterOneReturnsOneItemOnly").from(3,5)
                .unsubscribeAfter(1).expect(2)
                // get test suites
                .testSuite(OperatorReduceTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }
    
    public void testSumWithNoInitialValue() {
        Func2<Integer, Integer, Integer> sum = new Func2<Integer,Integer,Integer>() {

            @Override
            public Integer call(Integer a, Integer b) {
                return a + b;
            }};
        int result = Observable.range(1,4).lift(OperatorReduce.create(sum)).toBlocking().single();
        assertEquals(10, result);
    }
    
    public void testSumWithEmpty() {
        Func2<Integer, Integer, Integer> sum = new Func2<Integer,Integer,Integer>() {

            @Override
            public Integer call(Integer a, Integer b) {
                return a + b;
            }};
        int result = Observable.range(1,4).lift(OperatorReduce.create(sum)).toBlocking().single();
        assertEquals(10, result);
    }

    private static final Func1<Observable<Integer>, Observable<Integer>> count = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return o.lift(OperatorReduce.create(0, new Func2<Integer,Integer,Integer>() {

                @Override
                public Integer call(Integer n, Integer o) {
                    return n+1;
                }}));
        }
    };

}

//    @Benchmark
//    public void rxJavaCount() {
//        Observable.range(1, 1000000)
//                .reduce(0, new Func2<Integer, Object, Integer>() {
//                    @Override
//                    public final Integer call(Integer t1, Object t2) {
//                        return t1 + 1;
//                    }
//                }).subscribe();
//    }
//
//    private static final class CountHolder {
//        static final Func2<Integer, Object, Integer> INSTANCE = new Func2<Integer, Object, Integer>() {
//            @Override
//            public final Integer call(Integer t1, Object t2) {
//                return t1 + 1;
//            }
//        };
//    }
//
//    @Benchmark
//    public void rxJavaCountConstant() {
//        Observable.range(1, 1000000).reduce(0, CountHolder.INSTANCE)
//                .subscribe();
//    }
//
//    @Benchmark
//    public void rxJavaCountNewReduce() {
//        Observable
//                .range(1, 1000000)
//                .lift(new OperatorReduce<Integer, Integer>(
//                        CountHolder.INSTANCE, 0)).subscribe();
//    }

