package com.github.davidmoten.rx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

public final class TestingHelper {

    public static <T, R> Builder<T, R> function(Func1<Observable<T>, Observable<R>> function) {
        return new Builder<T, R>().function(function);
    }

    private static class Case<T, R> {
        final List<T> from;
        final List<R> expected;

        Case(List<T> from, List<R> expected) {
            this.from = from;
            this.expected = expected;
        }
    }

    public static class Builder<T, R> {

        private final List<Case<T, R>> cases = new ArrayList<Case<T, R>>();

        private Func1<Observable<T>, Observable<R>> function;

        public ExpectBuilder<T, R> fromEmpty() {
            return new ExpectBuilder<T, R>(this, Collections.<T> emptyList());
        }

        public ExpectBuilder<T, R> from(T... items) {
            return new ExpectBuilder<T, R>(this, Arrays.asList(items));
        }

        public Builder<T, R> function(Func1<Observable<T>, Observable<R>> function) {
            this.function = function;
            return this;
        }

        public Builder<T, R> expect(List<T> from, List<R> expected) {
            cases.add(new Case<T, R>(from, expected));
            return this;
        }

        public void runTests() {
            for (Case<T, R> c : cases) {
                runTest(function, c);
            }
            System.out.println("tests passed");
        }
    }

    private static <T, R> void runTest(Func1<Observable<T>, Observable<R>> function, Case<T, R> c) {
        UnsubscribeDetector<T> detector = UnsubscribeDetector.detect();
        TestSubscriber<R> sub = new TestSubscriber<R>();
        function.call(Observable.from(c.from).lift(detector)).subscribe(sub);
        sub.assertTerminalEvent();
        sub.assertNoErrors();
        sub.assertReceivedOnNext(c.expected);
        sub.assertUnsubscribed();
        try {
            detector.latch().await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static class ExpectBuilder<T, R> {
        private final boolean empty;
        private List<T> list;
        private final Builder<T, R> builder;

        private ExpectBuilder(Builder<T, R> builder, List<T> list) {
            this.builder = builder;
            this.list = list;
            this.empty = false;
        }

        private ExpectBuilder(Builder<T, R> builder, boolean empty) {
            this.empty = empty;
            this.builder = builder;
        }

        public Builder<T, R> expectEmpty() {
            return builder.expect(list, Collections.<R> emptyList());
        }

        public Builder<T, R> expect(R... items) {
            return expect(Arrays.asList(items));
        }

        public Builder<T, R> expect(List<R> items) {
            return builder.expect(list, items);
        }

        public Builder<T, R> expect(Set<R> set) {
            return new Builder<T, R>();
        }
    }

    private static class MyTestSuite<T, R> extends TestSuite {

        MyTestSuite(List<Case<T, R>> cases) {
            for (Case<T, R> c : cases) {
                addTest(new MyTestCase(c));
            }
        }
    }

    private static class MyTestCase<T, R> extends TestCase {

        private final Case<T, R> c;

        MyTestCase(Case<T, R> c) {
            this.c = c;
        }

        @Override
        protected void runTest() throws Throwable {

        }

    }

    public static void main(String[] args) {
        // test count operator
        Func1<Observable<String>, Observable<Integer>> f = new Func1<Observable<String>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<String> o) {
                return o.count();
            }
        };
        TestingHelper.function(f)
        // test empty
                .fromEmpty().expect(0)
                // test non-empty count
                .from("a", "b").expect(2)
                // test single input
                .from("a").expect(1)
                // run tests
                .runTests();

    }
}
