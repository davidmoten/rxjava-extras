package com.github.davidmoten.rx.internal.operators;

import java.io.File;

import com.github.davidmoten.rx.Strings;
import com.github.davidmoten.rx.testing.TestingHelper;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

public class StringsMoreTest extends TestCase {

    public static TestSuite suite() {

        return TestingHelper.function(STRINGS)
                // test empty
                .name("testStringsFromNoFile").fromEmpty().expectEmpty().name("testStringsFromFile")
                .from("src/test/resources/test1.txt").expect("hello there how\n" + "are you?")
                // get suite
                .testSuite(StringsMoreTest.class);
    }

    public void testDummy() {
        // keep eclipse happy
    }

    private static final Func1<Observable<String>, Observable<String>> STRINGS = new Func1<Observable<String>, Observable<String>>() {

        @Override
        public Observable<String> call(Observable<String> o) {
            return o.flatMap(new Func1<String, Observable<String>>() {

                @Override
                public Observable<String> call(String filename) {
                    return Strings.from(new File(filename));
                }
            });
        }
    };
}
