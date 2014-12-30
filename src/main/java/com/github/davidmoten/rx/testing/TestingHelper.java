package com.github.davidmoten.rx.testing;

import static com.github.davidmoten.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

    private static Optional<Long> ABSENT = Optional.absent();

    public static <T, R> Builder<T, R> function(Func1<Observable<T>, Observable<R>> function) {
        return new Builder<T, R>().function(function);
    }

    public static class Builder<T, R> {

        private static final String TEST_UNNAMED = "testUnnamed";
        private final List<Case<T, R>> cases = new ArrayList<Case<T, R>>();
        private Func1<Observable<T>, Observable<R>> function;
        private long waitForUnusbscribeMs = 100;
        private long waitForTerminalEventMs = 10000;
        private long waitForMoreTerminalEventsMs = 50;

        private Builder() {
            // must instantiate via TestingHelper.function method above
        }

        public Builder<T, R> function(Func1<Observable<T>, Observable<R>> function) {
            this.function = function;
            return this;
        }

        public Builder<T, R> waitForUnsubscribe(long duration, TimeUnit unit) {
            waitForUnusbscribeMs = unit.toMillis(duration);
            return this;
        }

        public Builder<T, R> waitForTerminalEvent(long duration, TimeUnit unit) {
            waitForTerminalEventMs = unit.toMillis(duration);
            return this;
        }

        public Builder<T, R> waitForMoreTerminalEvents(long duration, TimeUnit unit) {
            waitForMoreTerminalEventsMs = unit.toMillis(duration);
            return this;
        }

        public CaseBuilder<T, R> name(String name) {
            return new CaseBuilder<T, R>(this, Observable.<T> empty(), name);
        }

        public CaseBuilder<T, R> fromEmpty() {
            return new CaseBuilder<T, R>(this, Observable.<T> empty(), TEST_UNNAMED);
        }

        public CaseBuilder<T, R> from(T... items) {
            return new CaseBuilder<T, R>(this, Observable.from(items), TEST_UNNAMED);
        }

        public TestSuite testSuite(Class<?> cls) {
            return new AbstractTestSuite<T, R>(cls, new ArrayList<Case<T, R>>(this.cases));
        }

        Builder<T, R> expect(Observable<T> from, List<R> expected, boolean ordered,
                Optional<Long> expectSize, boolean checkSourceUnsubscribed, String name,
                Optional<Integer> unsubscribeAfter, Optional<Class<? extends Throwable>> expectError) {
            cases.add(new Case<T, R>(from, of(expected), ordered, expectSize,
                    checkSourceUnsubscribed, function, name, unsubscribeAfter, expectError,
                    waitForUnusbscribeMs, waitForTerminalEventMs, waitForMoreTerminalEventsMs));
            return this;
        }

    }

    public static class CaseBuilder<T, R> {
        private final Builder<T, R> builder;
        private String name;
        private Observable<T> from;
        private boolean checkSourceUnsubscribed = true;
        private Optional<Integer> unsubscribeAfter = Optional.absent();

        private CaseBuilder(Builder<T, R> builder, Observable<T> from, String name) {
            this.builder = builder;
            this.from = from;
            this.name = name;
        }

        public CaseBuilder<T, R> name(String name) {
            this.name = name;
            return this;
        }

        public CaseBuilder<T, R> fromEmpty() {
            from = Observable.empty();
            return this;
        }

        public CaseBuilder<T, R> from(T... items) {
            from = Observable.from(items);
            return this;
        }

        public CaseBuilder<T, R> from(Observable<T> items) {
            from = items;
            return this;
        }

        public CaseBuilder<T, R> fromError() {
            from = Observable.error(new TestingException());
            return this;
        }

        public CaseBuilder<T, R> fromErrorAfter(T... items) {
            from = Observable.from(items).concatWith(Observable.<T> error(new TestingException()));
            return this;
        }

        public CaseBuilder<T, R> fromErrorAfter(Observable<T> items) {
            from = items;
            return this;
        }

        public CaseBuilder<T, R> skipUnsubscribedCheck() {
            this.checkSourceUnsubscribed = false;
            return this;
        }

        public Builder<T, R> expectEmpty() {
            return expect(Collections.<R> emptyList());
        }

        public Builder<T, R> expectError() {
            return expectError(TestingException.class);
        }

        @SuppressWarnings("unchecked")
        public Builder<T, R> expectError(Class<? extends Throwable> cls) {
            return builder.expect(from, Collections.<R> emptyList(), true, ABSENT,
                    checkSourceUnsubscribed, name, unsubscribeAfter,
                    (Optional<Class<? extends Throwable>>) (Optional<?>) of(cls));
        }

        public Builder<T, R> expect(R... items) {
            return expect(Arrays.asList(items));
        }

        public Builder<T, R> expectSize(long n) {
            return builder.expect(from, Collections.<R> emptyList(), true, of(n),
                    checkSourceUnsubscribed, name, unsubscribeAfter,
                    Optional.<Class<? extends Throwable>> absent());
        }

        public Builder<T, R> expect(List<R> items) {
            return expect(items, true);
        }

        private Builder<T, R> expect(List<R> items, boolean ordered) {
            return builder.expect(from, items, ordered, ABSENT, checkSourceUnsubscribed, name,
                    unsubscribeAfter, Optional.<Class<? extends Throwable>> absent());
        }

        public Builder<T, R> expect(Set<R> set) {
            throw new RuntimeException();
        }

        public Builder<T, R> expectAnyOrder(R... items) {
            return expect(Arrays.asList(items), false);
        }

        public CaseBuilder<T, R> unsubscribeAfter(int n) {
            unsubscribeAfter = of(n);
            return this;
        }

    }

    private static class Case<T, R> {
        final String name;
        final Observable<T> from;
        final Optional<List<R>> expected;
        final boolean checkSourceUnsubscribed;
        final Func1<Observable<T>, Observable<R>> function;
        final Optional<Integer> unsubscribeAfter;
        final boolean ordered;
        final Optional<Long> expectSize;
        final Optional<Class<? extends Throwable>> expectError;
        final long waitForUnusbscribeMs;
        final long waitForTerminalEventMs;
        final long waitForMoreTerminalEventsMs;

        Case(Observable<T> from, Optional<List<R>> expected, boolean ordered,
                Optional<Long> expectSize, boolean checkSourceUnsubscribed,
                Func1<Observable<T>, Observable<R>> function, String name,
                Optional<Integer> unsubscribeAfter,
                Optional<Class<? extends Throwable>> expectError, long waitForUnusbscribeMs,
                long waitForTerminalEventMs, long waitForMoreTerminalEventsMs) {
            this.from = from;
            this.expected = expected;
            this.ordered = ordered;
            this.expectSize = expectSize;
            this.checkSourceUnsubscribed = checkSourceUnsubscribed;
            this.function = function;
            this.name = name;
            this.unsubscribeAfter = unsubscribeAfter;
            this.expectError = expectError;
            this.waitForUnusbscribeMs = waitForUnusbscribeMs;
            this.waitForTerminalEventMs = waitForTerminalEventMs;
            this.waitForMoreTerminalEventsMs = waitForMoreTerminalEventsMs;
        }
    }

    private static <T, R> void runTest(Case<T, R> c, TestType testType) {
        UnsubscribeDetector<T> detector = UnsubscribeDetector.create();
        MyTestSubscriber<R> sub = createTestSubscriber(testType, c.unsubscribeAfter);
        c.function.call(c.from.lift(detector)).subscribe(sub);
        if (c.unsubscribeAfter.isPresent()) {
            waitForUnsubscribe(detector, c.waitForUnusbscribeMs, TimeUnit.MILLISECONDS);
            sub.assertNoErrors();
        } else {
            sub.awaitTerminalEvent(c.waitForTerminalEventMs, TimeUnit.MILLISECONDS);
            if (c.expectError.isPresent()) {
                sub.assertError(c.expectError.get());
                // wait for more terminal events
                pause(c.waitForMoreTerminalEventsMs, TimeUnit.MILLISECONDS);
                assertEquals(0, sub.numOnCompletedEvents());
            } else {
                sub.assertNoErrors();
                // wait for more terminal events
                pause(c.waitForMoreTerminalEventsMs, TimeUnit.MILLISECONDS);
                assertEquals(1, sub.numOnCompletedEvents());
                sub.assertNoErrors();
            }
        }

        if (c.expected.isPresent())
            sub.assertReceivedOnNext(c.expected.get(), c.ordered);
        if (c.expectSize.isPresent())
            sub.assertReceivedCountIs(c.expectSize.get());
        else
            sub.assertUnsubscribed();
        if (c.checkSourceUnsubscribed)
            waitForUnsubscribe(detector, c.waitForUnusbscribeMs, TimeUnit.MILLISECONDS);
    }

    private static void pause(long duration, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(duration));
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    private static <T> void waitForUnsubscribe(UnsubscribeDetector<T> detector, long duration,
            TimeUnit unit) {
        try {
            assertTrue(detector.latch().await(duration, unit));
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    private static class TestingException extends RuntimeException {

        private static final long serialVersionUID = 4467514769366847747L;

    }

    public static class DeliveredMoreThanRequestedException extends RuntimeException {
        private static final long serialVersionUID = 1369440545774454215L;

        public DeliveredMoreThanRequestedException() {
            super("more items arrived than requested");
        }
    }

    private static final class MyTestSubscriber<T> extends Subscriber<T> {

        private final List<T> next = new ArrayList<T>();
        private final Optional<Long> onStartRequest;
        private final Optional<Long> onStartRequest2;
        private final Optional<Long> onNextRequest;
        private final Optional<Integer> unsubscribeAfter;
        private final CountDownLatch terminalLatch;
        private int completed = 0;
        private int count = 0;
        private int errors = 0;
        private final AtomicLong expected = new AtomicLong();
        private Optional<Throwable> lastError = Optional.absent();

        MyTestSubscriber(Optional<Integer> unsubscribeAfter, final Optional<Long> onStartRequest,
                final Optional<Long> onStartRequest2, final Optional<Long> onNextRequest) {
            this.unsubscribeAfter = unsubscribeAfter;
            this.onStartRequest = onStartRequest;
            this.onStartRequest2 = onStartRequest2;
            this.onNextRequest = onNextRequest;
            this.terminalLatch = new CountDownLatch(1);
        }

        MyTestSubscriber(Optional<Integer> unsubscribeAfter) {
            this(unsubscribeAfter, ABSENT, ABSENT, ABSENT);
        }

        @Override
        public void onStart() {
            if (!onStartRequest.isPresent() && !onStartRequest2.isPresent())
                // if nothing requested in onStart then must be requesting all
                expected.set(Long.MAX_VALUE);
            else
                expected.set(0);
            if (onStartRequest.isPresent())
                requestMore(onStartRequest.get());
            if (onStartRequest2.isPresent())
                requestMore(onStartRequest2.get());
        }

        private void requestMore(long n) {
            if (expected.get() != Long.MAX_VALUE) {
                if (n > 0)
                    expected.addAndGet(n);
                request(n);
            }
        }

        @Override
        public void onCompleted() {
            completed++;
            terminalLatch.countDown();
        }

        @Override
        public void onError(Throwable e) {
            errors++;
            lastError = of(e);
            terminalLatch.countDown();
        }

        @Override
        public void onNext(T t) {
            final long exp;
            if (expected.get() != Long.MAX_VALUE)
                exp = expected.decrementAndGet();
            else
                exp = expected.get();
            next.add(t);
            count++;
            if (exp < 0)
                onError(new DeliveredMoreThanRequestedException());
            else if (unsubscribeAfter.isPresent() && count == unsubscribeAfter.get())
                unsubscribe();
            else if (onNextRequest.isPresent() && exp == 0)
                requestMore(onNextRequest.get());
        }

        void assertError(Class<?> cls) {
            Assert.assertEquals(1, errors);
            Assert.assertTrue(cls.isInstance(lastError.get()));
        }

        void assertReceivedCountIs(long count) {
            Assert.assertEquals(count, next.size());
        }

        void awaitTerminalEvent(long duration, TimeUnit unit) {
            try {
                assertTrue(terminalLatch.await(duration, unit));
            } catch (InterruptedException e) {
            }
        }

        void assertReceivedOnNext(List<T> expected, boolean ordered) {
            TestingHelper.equals(expected, next, ordered);
        }

        void assertUnsubscribed() {
            assertTrue(super.isUnsubscribed());
        }

        int numOnCompletedEvents() {
            return completed;
        }

        void assertNoErrors() {
            if (errors > 0)
                lastError.get().printStackTrace();
            assertEquals(0, errors);
        }

    }

    private static enum TestType {
        WITHOUT_BACKP, BACKP_INITIAL_REQUEST_MAX, BACKP_INITIAL_REQUEST_MAX_THEN_BY_ONE, BACKP_ONE_BY_ONE, BACKP_TWO_BY_TWO, BACKP_REQUEST_ZERO, BACKP_REQUEST_NEGATIVE, BACKP_FIVE_BY_FIVE, BACKP_FIFTY_BY_FIFTY, BACKP_THOUSAND_BY_THOUSAND;
    }

    private static <T> MyTestSubscriber<T> createTestSubscriber(Optional<Integer> unsubscribeAfter,
            long onStartRequest, Optional<Long> onNextRequest) {
        return new MyTestSubscriber<T>(unsubscribeAfter, of(onStartRequest), ABSENT, onNextRequest);
    }

    private static <T> MyTestSubscriber<T> createTestSubscriber(TestType testType,
            final Optional<Integer> unsubscribeAfter) {

        if (testType == TestType.WITHOUT_BACKP)
            return new MyTestSubscriber<T>(unsubscribeAfter);
        else if (testType == TestType.BACKP_INITIAL_REQUEST_MAX)
            return createTestSubscriber(unsubscribeAfter, Long.MAX_VALUE, ABSENT);
        else if (testType == TestType.BACKP_INITIAL_REQUEST_MAX_THEN_BY_ONE)
            return createTestSubscriber(unsubscribeAfter, Long.MAX_VALUE, of(1L));
        else if (testType == TestType.BACKP_ONE_BY_ONE)
            return createTestSubscriber(unsubscribeAfter, 1L, of(1L));
        else if (testType == TestType.BACKP_REQUEST_ZERO)
            return new MyTestSubscriber<T>(unsubscribeAfter, of(0L), of(1L), of(1L));
        else if (testType == TestType.BACKP_REQUEST_NEGATIVE)
            return new MyTestSubscriber<T>(unsubscribeAfter, of(-1000L), of(1L), of(1L));
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

    private static <T> MyTestSubscriber<T> createTestSubscriberWithBackpNbyN(
            final long requestSize, final Optional<Integer> unsubscribeAfter) {
        return new MyTestSubscriber<T>(unsubscribeAfter, of(requestSize), ABSENT, of(requestSize));
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
