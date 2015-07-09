package com.github.davidmoten.rx.operators;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.testing.TestingHelper;

public class OperatorOrderedMergeHelperTest extends TestCase {

    public static TestSuite suite() {
        return TestingHelper
        // sync
                .function(f)
                //
                .name("testOrderedMerge").from(1, 2, 4, 10).expect(1, 2, 3, 4, 5, 10, 11)
                //
                .name("testOrderedMergeAllBeforeOther").from(1, 2).expect(1, 2, 3, 5, 11)
                //
                .name("testOrderedMergeOneBeforeOther").from(1).expect(1, 3, 5, 11)
                //
                .name("testOrderedMergeEmptyBeforeOther").fromEmpty().expect(3, 5, 11)
                //
                .name("testOrderedMergeAllAfterOther").from(12, 13).expect(3, 5, 11, 12, 13)
                // get test suites
                .testSuite(OperatorBufferEmissionsHelperTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }

    private static Func1<Observable<Integer>, Observable<Integer>> f = new Func1<Observable<Integer>, Observable<Integer>>() {

        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return o.compose(Transformers.mergeOrderedWith(Observable.just(3, 5, 11),
                    OperatorOrderedMergeTest.comparator));
        }
    };
}
