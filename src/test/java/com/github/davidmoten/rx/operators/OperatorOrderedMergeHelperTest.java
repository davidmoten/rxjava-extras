package com.github.davidmoten.rx.operators;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.testing.TestingHelper;

public class OperatorOrderedMergeHelperTest extends TestCase {

    public static TestSuite suite() {
        return TestingHelper
        // sync
                .function(f)
                //
                .name("testOrderedMerge").expect(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                // get test suites
                .testSuite(OperatorBufferEmissionsHelperTest.class);
    }

    private static final Func2<Integer, Integer, Integer> comparator = new Func2<Integer, Integer, Integer>() {

        @Override
        public Integer call(Integer t1, Integer t2) {
            return t1.compareTo(t2);
        }
    };

    private static Func1<Observable<Integer>, Observable<Integer>> f = new Func1<Observable<Integer>, Observable<Integer>>() {

        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return Observable.just(1, 4, 7, 10).compose(
                    Transformers.orderedMergeWith(Observable.just(2, 3, 5, 6, 8, 9), comparator));
        }
    };

    public void testDummy() {
        // just here to fool eclipse
    }
}
