package com.github.davidmoten.rx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

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
        final boolean checkSourceUnsubscribed;
        final Func1<Observable<T>, Observable<R>> function;
        final String name;

        Case(List<T> from, List<R> expected, boolean checkSourceUnsubscribed,
                Func1<Observable<T>, Observable<R>> function, String name) {
            this.from = from;
            this.expected = expected;
            this.checkSourceUnsubscribed = checkSourceUnsubscribed;
            this.function = function;
            this.name = name;
        }
    }

    public static class Builder<T, R> {

        private static final String TEST_UNNAMED = "testUnnamed";

        private final List<Case<T, R>> cases = new ArrayList<Case<T, R>>();

        private Func1<Observable<T>, Observable<R>> function;

        public ExpectBuilder<T, R> fromEmpty() {
            return new ExpectBuilder<T, R>(this, Collections.<T> emptyList(), TEST_UNNAMED);
        }

        public ExpectBuilder<T, R> name(String name) {
            return new ExpectBuilder<T, R>(this, Collections.<T> emptyList(), name);
        }

        public ExpectBuilder<T, R> from(T... items) {
            return new ExpectBuilder<T, R>(this, Arrays.asList(items), TEST_UNNAMED);
        }

        public Builder<T, R> function(Func1<Observable<T>, Observable<R>> function) {
            this.function = function;
            return this;
        }

        public Builder<T, R> expect(List<T> from, List<R> expected,
                boolean checkSourceUnsubscribed, String name) {
            cases.add(new Case<T, R>(from, expected, checkSourceUnsubscribed, function, name));
            return this;
        }

        public TestSuite testSuite() {
            return new AbstractTestSuite<T, R>(this);
        }

        public void runTests() {
            for (Case<T, R> c : cases) {
                runTest(c);
            }
            System.out.println("tests passed");
        }
    }

    private static <T, R> void runTest(Case<T, R> c) {
        UnsubscribeDetector<T> detector = UnsubscribeDetector.detect();
        TestSubscriber<R> sub = new TestSubscriber<R>();
        c.function.call(Observable.from(c.from).lift(detector)).subscribe(sub);
        sub.assertTerminalEvent();
        sub.assertNoErrors();
        sub.assertReceivedOnNext(c.expected);
        sub.assertUnsubscribed();
        if (c.checkSourceUnsubscribed)
            try {
                detector.latch().await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // do nothing
            }
    }

    public static class ExpectBuilder<T, R> {
        private List<T> list;
        private final Builder<T, R> builder;
        private boolean checkSourceUnsubscribed = true;
        private String name;

        private ExpectBuilder(Builder<T, R> builder, List<T> list, String name) {
            this.builder = builder;
            this.list = list;
            this.name = name;
        }

        public ExpectBuilder<T, R> skipUnsubscribedCheck() {
            this.checkSourceUnsubscribed = false;
            return this;
        }

        public ExpectBuilder<T, R> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T, R> expectEmpty() {
            return builder.expect(list, Collections.<R> emptyList(), checkSourceUnsubscribed, name);
        }

        public Builder<T, R> expect(R... items) {
            return expect(Arrays.asList(items));
        }

        public Builder<T, R> expect(List<R> items) {
            return builder.expect(list, items, checkSourceUnsubscribed, name);
        }

        public Builder<T, R> expect(Set<R> set) {
            throw new RuntimeException();
        }

        public ExpectBuilder<T, R> fromEmpty() {
            list = Collections.emptyList();
            return this;
        }

        public ExpectBuilder<T, R> from(T... items) {
            list = Arrays.asList(items);
            return this;
        }

    }

    @RunWith(Suite.class)
    public static class AbstractTestSuite<T, R> extends TestSuite {

        AbstractTestSuite(Builder<T, R> builder) {
            super();
            int i = 0;
            for (Case<T, R> c : builder.cases) {
                addTest(new MyTestCase<T, R>(c.name, c));
            }
        }
    }

    private static class MyTestCase<T, R> extends TestCase {

        private final Case<T, R> c;

        MyTestCase(String name, Case<T, R> c) {
            super(name);
            this.c = c;
        }

        @Override
        protected void runTest() throws Throwable {
            TestingHelper.runTest(c);
        }

    }

}
