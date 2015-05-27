package com.github.davidmoten.rx.operators;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;

import com.github.davidmoten.rx.Operators;
import com.github.davidmoten.rx.testing.TestingHelper;
import com.github.davidmoten.rx.testing.TestingHelperConcatTest;

public class OperatorBufferRequestsTest extends TestCase {

    public static TestSuite suite() {
        return TestingHelper.function(BUFFER_SYNC)
        // test emptys
                .name("testFiveSync").expect(1, 2, 3, 4, 5)
                .function(BUFFER_ASYNC)
                .name("testFiveAsync").expect(1, 2, 3, 4, 5)
                // get test suites
                .testSuite(TestingHelperConcatTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }

    private static final Func1<Observable<Integer>, Observable<Integer>> BUFFER_SYNC = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return BUFFER_BASE.call(o).lift(Operators.<Integer>bufferEmissions());
        }
    };
    
    private static final Func1<Observable<Integer>, Observable<Integer>> BUFFER_ASYNC = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return BUFFER_BASE.call(o).lift(Operators.<Integer>bufferEmissionsAsyncOptimized());
        }
    };
    
    private static final Func1<Observable<Integer>, Observable<Integer>> BUFFER_BASE = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return o.concatWith(Observable.create(new OnSubscribe<Integer>() {

                @Override
                public void call(Subscriber<? super Integer> subscriber) {
                    for (int i = 1; i <= 5; i++)
                        subscriber.onNext(i);
                    subscriber.onCompleted();
                }

            }));
        }
    };
}
