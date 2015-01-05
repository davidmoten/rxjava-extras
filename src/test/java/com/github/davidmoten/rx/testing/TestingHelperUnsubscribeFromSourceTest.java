package com.github.davidmoten.rx.testing;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

public class TestingHelperUnsubscribeFromSourceTest extends TestCase {

    private static final Func1<Observable<Integer>, Observable<Integer>> NO_UNSUBSCRIBE = new Func1<Observable<Integer>, Observable<Integer>>() {

        @Override
        public Observable<Integer> call(Observable<Integer> source) {
            return Observable.just(1);
        }
    };

    public static TestSuite suite() {
        return TestingHelper.function(NO_UNSUBSCRIBE).name("testUnsubscribeSource").from(1, 2)
                .skipUnsubscribedCheck().expect(1)
                .testSuite(TestingHelperUnsubscribeFromSourceTest.class);
    }

    public void testDummy() {
    }

}
