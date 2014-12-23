package com.github.davidmoten.rx;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

public class OperatorCountTest extends TestCase {

    private static final Func1<Observable<String>, Observable<Integer>> COUNT = new Func1<Observable<String>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<String> o) {
            return o.count();
        }
    };

    public static TestSuite suite() {

        return TestingHelper.function(COUNT)
        // test empty
                .name("testEmpty").fromEmpty().expect(0)
                // test non-empty count
                .from("a", "b").name("testTwo").expect(2)
                // test single input
                .from("a").name("testOne").expect(1)
                // get test suite
                .testSuite();
    }
}
