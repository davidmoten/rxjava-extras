package com.github.davidmoten.rx.testing;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

public class TestingHelperCountTest extends TestCase {

    public static TestSuite suite() {
        return TestingHelper.function(COUNT)
                // test empty
                .name("testCountOfEmptyReturnsZero").fromEmpty().expect(0)
                // test error
                .name("testCountErrorReturnsError").fromError().expectError()
                // test error after some emission
                .name("testCountErrorAfterTwoEmissionsReturnsError").fromErrorAfter(5, 6)
                .expectError()
                // test non-empty count
                .name("testCountOfTwoReturnsTwo").from(5, 6).expect(2)
                // test single input
                .name("testCountOfOneReturnsOne").from(5).expect(1)
                // count many
                .name("testCountOfManyDoesNotGiveStackOverflow").from(Observable.range(1, 1000000))
                .expect(1000000)
                // get test suites
                .testSuite(TestingHelperCountTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }

    public void testCountUnsubscribe() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Observable.from(Arrays.asList(1, 2)).doOnUnsubscribe(new Action0() {

            @Override
            public void call() {
                latch.countDown();
            }
        }).count().subscribe();
        assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    }

    private static final Func1<Observable<Integer>, Observable<Integer>> COUNT = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return o.count();
        }
    };

}
