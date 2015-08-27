package com.github.davidmoten.rx.internal.operators;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import org.junit.Test;

import com.github.davidmoten.rx.internal.operators.OperatorBufferEmissions;

import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;

public class StringSplitOperatorTest {

    @Test
    public void testNormal() {
        Observable<String> o = Observable.just("boo:an", "d:you");
        List<String> expected = asList("boo", "and", "you");
        check(o, expected);
    }

    @Test
    public void testNormalWithBackpressure() throws InterruptedException {
        Observable<String> o = Observable.just("boo:an", "d:you");
        List<String> expected = asList("boo", "and", "you");
        checkWithBackpressure(o, expected);
    }

    @Test
    public void testEmptyProducesNothing() {
        Observable<String> o = Observable.empty();
        List<String> expected = asList();
        check(o, expected);
    }

    @Test
    public void testEmptyProducesNothingWithBackpressure() {
        Observable<String> o = Observable.empty();
        List<String> expected = asList();
        checkWithBackpressure(o, expected);
    }

    @Test
    public void testBlankProducesSingleBlank() {
        Observable<String> o = Observable.just("");
        List<String> expected = asList("");
        check(o, expected);
    }

    @Test
    public void testBlankProducesBlankWithBackpressure() {
        Observable<String> o = Observable.just("");
        List<String> expected = asList("");
        checkWithBackpressure(o, expected);
    }

    @Test
    public void testNoSeparatorProducesSingle() {
        Observable<String> o = Observable.just("abc");
        List<String> expected = asList("abc");
        check(o, expected);
    }

    @Test
    public void testNoSeparatorProducesSingleWithBackpressure() {
        Observable<String> o = Observable.just("abc");
        List<String> expected = asList("abc");
        checkWithBackpressure(o, expected);
    }

    @Test
    public void testSeparatorOnlyProducesTwoBlanks() {
        Observable<String> o = Observable.just(":");
        List<String> expected = asList("", "");
        check(o, expected);
    }

    @Test
    public void testSeparatorOnlyProducesTwoBlanksWithBackpressure() {
        Observable<String> o = Observable.just(":");
        List<String> expected = asList("", "");
        checkWithBackpressure(o, expected);
    }

    @Test
    public void testEmptyItemsAtEndEmitted() {
        Observable<String> o = Observable.just("::boo:an", "d:::you::");
        List<String> expected = asList("", "", "boo", "and", "", "", "you", "", "");
        check(o, expected);
    }

    @Test
    public void testEmptyItemsAtEndEmittedWithBackpressure() {
        Observable<String> o = Observable.just("::boo:an", "d:::you::");
        List<String> expected = asList("", "", "boo", "and", "", "", "you", "", "");
        checkWithBackpressure(o, expected);
    }

    @Test
    public void testSplitOperatorDoesNotStallDueToInsufficientUpstreamRequests() {
        Observable<String> o = Observable.just("hello", "there", ":how");
        List<String> expected = asList("hellothere", "how");
        checkWithBackpressure(o, expected);
    }

    @Test
    public void testBackpressureOneByOneWithBufferEmissions() {
        Observable<String> o = Observable.just("boo:an", "d:you")
                .lift(new StringSplitOperator(Pattern.compile(":")))
                .lift(new OperatorBufferEmissions<String>());
        TestSubscriber<String> ts = TestSubscriber.create(0);
        o.subscribe(ts);
        ts.requestMore(1);
        ts.assertValues("boo");
        ts.requestMore(1);
        ts.assertValues("boo", "and");
        ts.requestMore(1);
        ts.assertValues("boo", "and", "you");
    }

    private static void checkWithBackpressure(Observable<String> o, List<String> expected) {
        final List<String> list = new ArrayList<String>();
        o.lift(new StringSplitOperator(Pattern.compile(":")))
                .subscribe(createBackpressureSubscriber(list));
        assertEquals(expected, list);
    }

    private static void check(Observable<String> o, List<String> expected) {
        List<String> list = o.lift(new StringSplitOperator(Pattern.compile(":"))).toList()
                .toBlocking().single();
        assertEquals(expected, list);
    }

    private static Subscriber<String> createBackpressureSubscriber(final List<String> list) {
        final CountDownLatch latch = new CountDownLatch(1);
        return new Subscriber<String>() {

            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            @Override
            public void onNext(String s) {
                list.add(s);
                request(1);
            }
        };
    }
}
