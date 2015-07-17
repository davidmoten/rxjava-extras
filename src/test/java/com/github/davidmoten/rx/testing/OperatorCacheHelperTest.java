package com.github.davidmoten.rx.testing;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class OperatorCacheHelperTest extends TestCase {

    private static final Func1<Observable<Integer>, Observable<Integer>> FUNCTION = new Func1<Observable<Integer>, Observable<Integer>>() {

        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            Observable<Integer> c = o.cache();
            c.delay(100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation()).subscribe();
            c.delay(50, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation()).subscribe();
            return c.delay(100, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.computation());
        }
    };

    public static TestSuite suite() {
        return TestingHelper.function(FUNCTION)
        // test empty
                .name("testCacheOfEmptyReturnsEmpty").fromEmpty().expectEmpty()
                //
                .name("testCacheOfSomeReturnsSome").from(1, 2, 3, 4, 5).expect(1, 2, 3, 4, 5)
                // get test suites
                .testSuite(TestingHelperCountTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }

}
