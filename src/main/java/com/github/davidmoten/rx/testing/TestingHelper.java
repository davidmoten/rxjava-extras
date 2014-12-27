package com.github.davidmoten.rx.testing;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import com.github.davidmoten.util.Optional;

public final class TestingHelper {

    public static <T, R> Builder<T, R> function(Func1<Observable<T>, Observable<R>> function) {
        return new Builder<T, R>().function(function);
    }

    public static class Builder<T, R> {

        private static final String TEST_UNNAMED = "testUnnamed";

        private final List<Case<T, R>> cases = new ArrayList<Case<T, R>>();

        private Func1<Observable<T>, Observable<R>> function;

        public CaseBuilder<T, R> fromEmpty() {
            return new CaseBuilder<T, R>(this, Collections.<T> emptyList(), TEST_UNNAMED);
        }

        public CaseBuilder<T, R> name(String name) {
            return new CaseBuilder<T, R>(this, Collections.<T> emptyList(), name);
        }

        public CaseBuilder<T, R> from(T... items) {
            return new CaseBuilder<T, R>(this, Arrays.asList(items), TEST_UNNAMED);
        }

        public Builder<T, R> function(Func1<Observable<T>, Observable<R>> function) {
            this.function = function;
            return this;
        }

        public Builder<T, R> expect(List<T> from, List<R> expected, boolean ordered,
                Optional<Long> expectSize, boolean checkSourceUnsubscribed, String name,
                Optional<Integer> unsubscribeAfter) {
            cases.add(new Case<T, R>(from, Optional.of(expected), ordered, expectSize,
                    checkSourceUnsubscribed, function, name, unsubscribeAfter));
            return this;
        }

        public TestSuite testSuite(Class<?> cls) {
            return new AbstractTestSuite<T, R>(cls, new ArrayList<Case<T, R>>(this.cases));
        }

    }

    public static class CaseBuilder<T, R> {
        private List<T> list;
        private final Builder<T, R> builder;
        private boolean checkSourceUnsubscribed = true;
        private Optional<Integer> unsubscribeAfter = Optional.absent();
        private String name;

        private CaseBuilder(Builder<T, R> builder, List<T> list, String name) {
            this.builder = builder;
            this.list = list;
            this.name = name;
        }

        public CaseBuilder<T, R> skipUnsubscribedCheck() {
            this.checkSourceUnsubscribed = false;
            return this;
        }

        public CaseBuilder<T, R> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T, R> expectEmpty() {
            return expect(Collections.<R> emptyList());
        }

        public Builder<T, R> expect(R... items) {
            return expect(Arrays.asList(items));
        }

        public Builder<T, R> expectSize(long n) {
            return builder.expect(list, Collections.<R> emptyList(), true, Optional.of(n),
                    checkSourceUnsubscribed, name, unsubscribeAfter);
        }

        public Builder<T, R> expect(List<R> items) {
            return expect(items, true);
        }

        private Builder<T, R> expect(List<R> items, boolean ordered) {
            return builder.expect(list, items, ordered, Optional.<Long> absent(),
                    checkSourceUnsubscribed, name, unsubscribeAfter);
        }

        public Builder<T, R> expect(Set<R> set) {
            throw new RuntimeException();
        }

        public CaseBuilder<T, R> fromEmpty() {
            list = Collections.emptyList();
            return this;
        }

        public CaseBuilder<T, R> from(T... items) {
            list = Arrays.asList(items);
            return this;
        }

        public CaseBuilder<T, R> unsubscribeAfter(int n) {
            unsubscribeAfter = Optional.of(n);
            return this;
        }

        public Builder<T, R> expectAnyOrder(R... items) {
            return expect(Arrays.asList(items), false);
        }

    }

    private static class Case<T, R> {
        final String name;
        final List<T> from;
        final Optional<List<R>> expected;
        final boolean checkSourceUnsubscribed;
        final Func1<Observable<T>, Observable<R>> function;
        final Optional<Integer> unsubscribeAfter;
        final boolean ordered;
        private final Optional<Long> expectSize;

        Case(List<T> from, Optional<List<R>> expected, boolean ordered, Optional<Long> expectSize,
                boolean checkSourceUnsubscribed, Func1<Observable<T>, Observable<R>> function,
                String name, Optional<Integer> unsubscribeAfter) {
            this.from = from;
            this.expected = expected;
            this.ordered = ordered;
            this.expectSize = expectSize;
            this.checkSourceUnsubscribed = checkSourceUnsubscribed;
            this.function = function;
            this.name = name;
            this.unsubscribeAfter = unsubscribeAfter;
        }
    }

    private static <T, R> void runTest(Case<T, R> c, TestType testType) {
        UnsubscribeDetector<T> detector = UnsubscribeDetector.create();
        MyTestSubscriber<R> sub = createTestSubscriber(testType, c.unsubscribeAfter);
        c.function.call(Observable.from(c.from).lift(detector)).subscribe(sub);
        if (c.unsubscribeAfter.isPresent()) {
            waitForUnsubscribe(detector);
            sub.assertNoErrors();
        } else {
            sub.awaitTerminalEvent();
            sub.assertNoErrors();
            // TODO might need a pause here to detect more completed events for
            // asynchronous sources
            assertEquals(1, sub.numOnCompletedEvents());
        }

        if (c.expected.isPresent())
            sub.assertReceivedOnNext(c.expected.get(), c.ordered);
        else if (c.expectSize.isPresent())
            sub.assertReceivedCountIs(c.expectSize.get());
        else
            sub.assertUnsubscribed();
        if (c.checkSourceUnsubscribed)
            waitForUnsubscribe(detector);
    }

    private static <T> void waitForUnsubscribe(UnsubscribeDetector<T> detector) {
        try {
            detector.latch().await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static class DeliveredMoreThanRequestedException extends RuntimeException {
        private static final long serialVersionUID = 1369440545774454215L;

        public DeliveredMoreThanRequestedException() {
            super("more items arrived than requested");
        }
    }

    private static class MyTestSubscriber<T> extends Subscriber<T> {

        private final Optional<Integer> unsubscribeAfter;
        private final CountDownLatch terminalLatch;

        MyTestSubscriber(Optional<Integer> unsubscribeAfter) {
            this.unsubscribeAfter = unsubscribeAfter;
            this.terminalLatch = new CountDownLatch(1);
        }

        public void assertReceivedCountIs(long count) {
            Assert.assertEquals(count, next.size());
        }

        public void awaitTerminalEvent() {
            try {
                terminalLatch.await();
            } catch (InterruptedException e) {
            }
        }

        int completed = 0;
        int errors = 0;
        int count = 0;
        final List<T> next = new ArrayList<T>();

        @Override
        public void onCompleted() {
            completed++;
            terminalLatch.countDown();
        }

        @Override
        public void onError(Throwable e) {
            errors++;
            terminalLatch.countDown();
            e.printStackTrace();
        }

        @Override
        public void onNext(T t) {
            next.add(t);
            count++;
            if (unsubscribeAfter.isPresent() && count == unsubscribeAfter.get())
                unsubscribe();
        }

        public void assertReceivedOnNext(List<T> expected, boolean ordered) {
            TestingHelper.equals(expected, next, ordered);
        }

        public void assertUnsubscribed() {
            Assert.assertTrue(super.isUnsubscribed());
        }

        public int numOnCompletedEvents() {
            return completed;
        }

        public void assertNoErrors() {
            Assert.assertEquals(0, errors);
        }

    }

    private static enum TestType {
        WITHOUT_BACKP, BACKP_INITIAL_REQUEST_MAX, BACKP_INITIAL_REQUEST_MAX_THEN_BY_ONE, BACKP_ONE_BY_ONE, BACKP_TWO_BY_TWO, BACKP_REQUEST_ZERO, BACKP_REQUEST_NEGATIVE, BACKP_FIVE_BY_FIVE, BACKP_FIFTY_BY_FIFTY, BACKP_THOUSAND_BY_THOUSAND;
    }

    private static <T> MyTestSubscriber<T> createTestSubscriber(
            final Optional<Integer> unsubscribeAfter, final Optional<Long> onStartRequest,
            final Optional<Long> onNextRequest) {
        return new MyTestSubscriber<T>(unsubscribeAfter) {

            @Override
            public void onStart() {
                if (onStartRequest.isPresent())
                    request(onStartRequest.get());
            }

            @Override
            public void onNext(T t) {
                super.onNext(t);
                if (onNextRequest.isPresent())
                    request(onNextRequest.get());
            }

        };

    }

    private static <T> MyTestSubscriber<T> createTestSubscriber(TestType testType,
            final Optional<Integer> unsubscribeAfter) {

        if (testType == TestType.WITHOUT_BACKP)
            return new MyTestSubscriber<T>(unsubscribeAfter);
        else if (testType == TestType.BACKP_INITIAL_REQUEST_MAX)
            return createTestSubscriber(unsubscribeAfter, Optional.of(Long.MAX_VALUE),
                    Optional.<Long> absent());
        else if (testType == TestType.BACKP_INITIAL_REQUEST_MAX_THEN_BY_ONE)
            // Hopefully doesn't cause a problem (for example by 1
            // getting added to Long.MAX_VALUE making it a negative
            // value because of overflow)
            return createTestSubscriber(unsubscribeAfter, Optional.of(Long.MAX_VALUE),
                    Optional.of(1L));
        else if (testType == TestType.BACKP_ONE_BY_ONE)
            return createTestSubscriber(unsubscribeAfter, Optional.of(1L), Optional.of(1L));
        else if (testType == TestType.BACKP_REQUEST_ZERO)
            return new MyTestSubscriber<T>(unsubscribeAfter) {

                @Override
                public void onStart() {
                    request(0);
                    request(1);
                }

                @Override
                public void onNext(T t) {
                    super.onNext(t);
                    request(1);
                }
            };
        else if (testType == TestType.BACKP_REQUEST_NEGATIVE)
            return new MyTestSubscriber<T>(unsubscribeAfter) {

                @Override
                public void onStart() {
                    request(-1000);
                    request(1);
                }

                @Override
                public void onNext(T t) {
                    super.onNext(t);
                    request(1);
                }
            };
        else if (testType == TestType.BACKP_REQUEST_NEGATIVE)
            return new MyTestSubscriber<T>(unsubscribeAfter) {

                @Override
                public void onStart() {
                    request(-1);
                    request(1);
                }

                @Override
                public void onNext(T t) {
                    super.onNext(t);
                    request(1);
                }
            };
        else if (testType == TestType.BACKP_TWO_BY_TWO)
            return createTestSubscriberWithBackpNbyN(2, unsubscribeAfter);
        else if (testType == TestType.BACKP_FIVE_BY_FIVE)
            return createTestSubscriberWithBackpNbyN(5, unsubscribeAfter);
        else if (testType == TestType.BACKP_FIFTY_BY_FIFTY)
            return createTestSubscriberWithBackpNbyN(2, unsubscribeAfter);
        else if (testType == TestType.BACKP_THOUSAND_BY_THOUSAND)
            return createTestSubscriberWithBackpNbyN(2, unsubscribeAfter);
        else
            throw new RuntimeException(testType + " not implemented");

    }

    private static <T> MyTestSubscriber<T> createTestSubscriberWithBackpNbyN(final int requestSize,
            final Optional<Integer> unsubscribeAfter) {
        return new MyTestSubscriber<T>(unsubscribeAfter) {
            long expected = 0;

            @Override
            public void onStart() {
                requestMore();
            }

            private void requestMore() {
                if (expected != Long.MAX_VALUE)
                    expected += requestSize;
                request(requestSize);
            }

            @Override
            public void onNext(T t) {
                if (expected != Long.MAX_VALUE)
                    expected--;
                super.onNext(t);
                if (expected < 0)
                    onError(new DeliveredMoreThanRequestedException());
                else if (expected == 0)
                    requestMore();
            }
        };
    }

    @RunWith(Suite.class)
    @SuiteClasses({})
    public static class AbstractTestSuite<T, R> extends TestSuite {

        AbstractTestSuite(Class<?> cls, List<Case<T, R>> cases) {
            super(cls);
            for (Case<T, R> c : cases) {
                for (TestType testType : TestType.values())
                    addTest(new MyTestCase<T, R>(c.name + "_" + testType.name(), c, testType));
            }
        }
    }

    private static class MyTestCase<T, R> extends TestCase {

        private final Case<T, R> c;
        private final TestType testType;

        MyTestCase(String name, Case<T, R> c, TestType testType) {
            super(name);
            this.c = c;
            this.testType = testType;
        }

        @Override
        protected void runTest() throws Throwable {
            TestingHelper.runTest(c, testType);
        }

    }

    private static <T> boolean equals(Collection<T> a, Collection<T> b, boolean ordered) {
        if (a.size() != b.size())
            return false;
        else if (ordered)
            return a.equals(b);
        else {
            List<T> list = new ArrayList<T>(a);
            for (T t : b) {
                if (!list.remove(t))
                    return false;
            }
            return true;
        }
    }
}
