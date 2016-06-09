package com.github.davidmoten.rx.testing;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

public class TestingHelperFilterTest extends TestCase {
    private static final Func1<Observable<Integer>, Observable<Integer>> FILTER = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return o.filter(new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer x) {
                    return x % 2 ==0;
                }
            });
        }
    };

    public static TestSuite suite() {
        return TestingHelper.function(FILTER)
                .name("testFilterOfEmptyReturnsEmpty").fromEmpty().expectEmpty() //
                .name("testFilterOfOddReturnsEmpty").from(1).expectEmpty() //
                .name("testFilterOfEventReturnsEven").from(2).expect(2) //
                .name("testListReturnsEvens").from(1,2,3,4,5,6,7).expect(2,4,6)
                .name("testErrorReturnsError").fromError().expectError() //
                // get test suites
                .testSuite(TestingHelperFilterTest.class);
    }
    
    
}
