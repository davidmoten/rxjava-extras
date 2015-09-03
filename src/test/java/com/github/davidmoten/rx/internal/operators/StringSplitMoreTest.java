package com.github.davidmoten.rx.internal.operators;

import com.github.davidmoten.rx.Strings;
import com.github.davidmoten.rx.testing.TestingHelper;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

public class StringSplitMoreTest extends TestCase {

    public static TestSuite suite() {
        return TestingHelper.function(SPLIT)
                // test empty
                .name("testEmpty").fromEmpty().expectEmpty()
                // normal
                .name("testNormal").from("boo:an", "d:you").expect("boo", "and", "you")
                // test empties
                .name("testEmptyItemsAtBeginningMiddleAndEndProduceBlanks")
                .from("::boo:an", "d:::you::").expect("", "", "boo", "and", "", "", "you", "", "")
                // test blank produces blank
                .name("testBlankProducesBlank").from("").expect("")
                // test
                .name("testNoSeparatorProducesSingle").from("and").expect("and")
                // test
                .name("testSeparatorOnlyProducesTwoBlanks").from(":").expect("", "")
                // get suite
                .testSuite(StringSplitMoreTest.class);
    }

    public void testDummy() {
        // keep eclipse happy
    }

    private static final Func1<Observable<String>, Observable<String>> SPLIT = new Func1<Observable<String>, Observable<String>>() {

        @Override
        public Observable<String> call(Observable<String> o) {
            return Strings.split(o, ":");
        }
    };
}
