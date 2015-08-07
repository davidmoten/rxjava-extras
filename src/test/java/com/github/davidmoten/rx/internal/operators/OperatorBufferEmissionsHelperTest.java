package com.github.davidmoten.rx.internal.operators;

import java.util.List;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.testing.TestingHelper;

public class OperatorBufferEmissionsHelperTest extends TestCase {

    public static TestSuite suite() {
        return TestingHelper
        // sync
                .function(BUFFER_SYNC)
                //
                .name("testHandlesOverproducingSourceWithSyncDrainer").expect(1, 2, 3, 4, 5)
                //
                // get test suites
                .testSuite(OperatorBufferEmissionsHelperTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }

    private static final Func1<Observable<Integer>, Observable<Integer>> BUFFER_SYNC = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return BUFFER_BASE.call(o).compose(Transformers.<Integer> bufferEmissions());
        }
    };

    private static final Func1<Observable<Integer>, Observable<Integer>> BUFFER_BASE = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return o.concatWith(Observable.just(1, 2, 3, 4, 5)).toList()
                    .lift(new Operator<Integer, List<Integer>>() {

                        @Override
                        public Subscriber<? super List<Integer>> call(
                                final Subscriber<? super Integer> child) {
                            Subscriber<List<Integer>> parent = new Subscriber<List<Integer>>() {

                                @Override
                                public void onStart() {
                                    request(1);
                                }

                                @Override
                                public void onCompleted() {
                                    child.onCompleted();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    child.onError(e);
                                }

                                @Override
                                public void onNext(List<Integer> list) {
                                    for (Integer n : list)
                                        child.onNext(n);
                                }

                            };
                            child.add(parent);
                            return parent;
                        }
                    });
        }
    };

    public static void main(String[] args) {
        Observable.just(1, 2, 3).concatWith(Observable.just(4)).subscribe();
    }

}
