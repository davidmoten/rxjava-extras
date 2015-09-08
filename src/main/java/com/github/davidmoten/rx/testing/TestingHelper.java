package com.github.davidmoten.rx.testing;

import static com.github.davidmoten.util.Optional.of;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;

import com.github.davidmoten.util.Optional;
import com.github.davidmoten.util.Preconditions;

/**
 * Testing utility functions.
 */
public final class TestingHelper {

    private static final Optional<Long> ABSENT = Optional.absent();

    /**
     * Sets the transformation to be tested and returns a builder to create test
     * cases.
     * 
     * @param function
     *            the transformation to be tested
     * @param <T>
     *            generic type of the from side of the transformation being
     *            tested
     * @param <R>
     *            generic type of the to side of the transformation being tested
     * @return builder for creating test cases
     */
    public static <T, R> Builder<T, R> function(Func1<Observable<T>, Observable<R>> function) {
        return new Builder<T, R>().function(function);
    }

    /**
     * Defines test cases and builds a JUnit test suite.
     * 
     * @param <T>
     *            generic type of the from side of the transformation being
     *            tested
     * @param <R>
     *            generic type of the to side of the transformation being tested
     */
    public static class Builder<T, R> {

        private final List<Case<T, R>> cases = new ArrayList<Case<T, R>>();
        private Func1<Observable<T>, Observable<R>> function;
        private long waitForUnusbscribeMs = 100;
        private long waitForTerminalEventMs = 10000;
        private long waitForMoreTerminalEventsMs = 50;

        private Builder() {
            // must instantiate via TestingHelper.function method above
        }

        /**
         * Sets transformation to be tested and returns the current builder.
         * 
         * @param function
         *            transformation to be tested
         * @return builder
         */
        public Builder<T, R> function(Func1<Observable<T>, Observable<R>> function) {
            Preconditions.checkNotNull(function, "function cannot be null");
            this.function = function;
            return this;
        }

        /**
         * Sets duration to wait for unusubscription to occur (either of source
         * or of downstream subscriber).
         * 
         * @param duration
         *            number of time units
         * @param unit
         *            time unit
         * @return builder
         */
        public Builder<T, R> waitForUnsubscribe(long duration, TimeUnit unit) {
            Preconditions.checkNotNull(unit, "unit cannot be null");
            waitForUnusbscribeMs = unit.toMillis(duration);
            return this;
        }

        /**
         * Sets duration to wait for a terminal event (completed or error) when
         * one is expected.
         * 
         * @param duration
         *            number of time units
         * @param unit
         *            time unit
         * @return builder
         */
        public Builder<T, R> waitForTerminalEvent(long duration, TimeUnit unit) {
            Preconditions.checkNotNull(unit, "unit cannot be null");
            waitForTerminalEventMs = unit.toMillis(duration);
            return this;
        }

        /**
         * Sets duration to wait for more terminal events after one has been
         * received.
         * 
         * @param duration
         *            number of time units
         * @param unit
         *            time unit
         * @return builder
         */
        public Builder<T, R> waitForMoreTerminalEvents(long duration, TimeUnit unit) {
            Preconditions.checkNotNull(unit, "unit cannot be null");
            waitForMoreTerminalEventsMs = unit.toMillis(duration);
            return this;
        }

        /**
         * Sets the name of the test which is used in the name of a junit test.
         * 
         * @param name
         *            name of the test
         * @return case builder
         */
        public CaseBuilder<T, R> name(String name) {
            Preconditions.checkNotNull(name, "name cannot be null");
            return new CaseBuilder<T, R>(this, Observable.<T> empty(), name);
        }

        /**
         * Returns the JUnit {@link TestSuite} comprised of the test cases
         * created so far. The cases will be listed under the root test named
         * according to the given class.
         * 
         * @param cls
         *            class corresponding to the tests root
         * @return test suite
         */
        public TestSuite testSuite(Class<?> cls) {
            Preconditions.checkNotNull(cls, "cls cannot be null");
            return new TestSuiteFromCases<T, R>(cls, new ArrayList<Case<T, R>>(this.cases));
        }

        private Builder<T, R> expect(Observable<T> from, Optional<List<R>> expected,
                boolean ordered, Optional<Long> expectSize, boolean checkSourceUnsubscribed,
                String name, Optional<Integer> unsubscribeAfter,
                Optional<Class<? extends Throwable>> expectError,
                Optional<Class<? extends RuntimeException>> expectException) {
            cases.add(new Case<T, R>(from, expected, ordered, expectSize, checkSourceUnsubscribed,
                    function, name, unsubscribeAfter, expectError, waitForUnusbscribeMs,
                    waitForTerminalEventMs, waitForMoreTerminalEventsMs, expectException));
            return this;
        }
    }

    public static class CaseBuilder<T, R> {
        private final Builder<T, R> builder;
        private String name;
        private Observable<T> from = Observable.empty();
        private boolean checkSourceUnsubscribed = true;
        private Optional<Integer> unsubscribeAfter = Optional.absent();

        private CaseBuilder(Builder<T, R> builder, Observable<T> from, String name) {
            Preconditions.checkNotNull(builder);
            Preconditions.checkNotNull(from);
            Preconditions.checkNotNull(name);
            this.builder = builder;
            this.from = from;
            this.name = name;
        }

        public CaseBuilder<T, R> name(String name) {
            Preconditions.checkNotNull(name, "name cannot be null");
            this.name = name;
            return this;
        }

        public CaseBuilder<T, R> fromEmpty() {
            from = Observable.empty();
            return this;
        }

        public CaseBuilder<T, R> from(T... source) {
            Preconditions.checkNotNull(source, "source cannot be null");
            from = Observable.from(source);
            return this;
        }

        public CaseBuilder<T, R> from(Observable<T> source) {
            Preconditions.checkNotNull(source, "source cannot be null");
            from = source;
            return this;
        }

        public CaseBuilder<T, R> fromError() {
            from = Observable.error(new TestingException());
            return this;
        }

        public CaseBuilder<T, R> fromErrorAfter(T... source) {
            Preconditions.checkNotNull(source, "source cannot be null");
            from = Observable.from(source).concatWith(Observable.<T> error(new TestingException()));
            return this;
        }

        public CaseBuilder<T, R> fromErrorAfter(Observable<T> source) {
            Preconditions.checkNotNull(source, "source cannot be null");
            from = source;
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
            Preconditions.checkNotNull(cls, "cls cannot be null");
            return builder.expect(from, Optional.<List<R>> absent(), true, ABSENT,
                    checkSourceUnsubscribed, name, unsubscribeAfter,
                    (Optional<Class<? extends Throwable>>) (Optional<?>) of(cls),
                    Optional.<Class<? extends RuntimeException>> absent());
        }

        public Builder<T, R> expect(R... source) {
            Preconditions.checkNotNull(source, "source cannot be null");
            return expect(Arrays.asList(source));
        }

        public Builder<T, R> expectSize(long n) {
            return builder.expect(from, Optional.<List<R>> absent(), true, of(n),
                    checkSourceUnsubscribed, name, unsubscribeAfter,
                    Optional.<Class<? extends Throwable>> absent(),
                    Optional.<Class<? extends RuntimeException>> absent());
        }

        public Builder<T, R> expect(List<R> source) {
            Preconditions.checkNotNull(source, "source cannot be null");
            return expect(source, true);
        }

        private Builder<T, R> expect(List<R> items, boolean ordered) {
            return builder.expect(from, of(items), ordered, ABSENT, checkSourceUnsubscribed, name,
                    unsubscribeAfter, Optional.<Class<? extends Throwable>> absent(),
                    Optional.<Class<? extends RuntimeException>> absent());
        }

        public Builder<T, R> expectAnyOrder(R... source) {
            Preconditions.checkNotNull(source, "source cannot be null");
            return expect(Arrays.asList(source), false);
        }

        public CaseBuilder<T, R> unsubscribeAfter(int n) {
            unsubscribeAfter = of(n);
            return this;
        }

        @SuppressWarnings("unchecked")
        public Builder<T, R> expectException(Class<? extends RuntimeException> cls) {
            return builder.expect(from, Optional.<List<R>> absent(), true, ABSENT,
                    checkSourceUnsubscribed, name, unsubscribeAfter,
                    Optional.<Class<? extends Throwable>> absent(),
                    (Optional<Class<? extends RuntimeException>>) (Optional<?>) Optional.of(cls));
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
        final Optional<Class<? extends RuntimeException>> expectedException;

        Case(Observable<T> from, Optional<List<R>> expected, boolean ordered,
                Optional<Long> expectSize, boolean checkSourceUnsubscribed,
                Func1<Observable<T>, Observable<R>> function, String name,
                Optional<Integer> unsubscribeAfter,
                Optional<Class<? extends Throwable>> expectError, long waitForUnusbscribeMs,
                long waitForTerminalEventMs, long waitForMoreTerminalEventsMs,
                Optional<Class<? extends RuntimeException>> expectedException) {
            Preconditions.checkNotNull(from);
            Preconditions.checkNotNull(expected);
            Preconditions.checkNotNull(expectSize);
            Preconditions.checkNotNull(function);
            Preconditions.checkNotNull(name);
            Preconditions.checkNotNull(unsubscribeAfter);
            Preconditions.checkNotNull(expectError);
            Preconditions.checkNotNull(expectedException);
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
            this.expectedException = expectedException;
        }
    }

    private static <T, R> void runTest(Case<T, R> c, TestType testType) {
        try {
            CountDownLatch sourceUnsubscribeLatch = new CountDownLatch(1);
            MyTestSubscriber<R> sub = createTestSubscriber(testType, c.unsubscribeAfter);
            c.function.call(c.from.doOnUnsubscribe(countDown(sourceUnsubscribeLatch)))
                    .subscribe(sub);
            if (c.unsubscribeAfter.isPresent()) {
                waitForUnsubscribe(sourceUnsubscribeLatch, c.waitForUnusbscribeMs,
                        TimeUnit.MILLISECONDS);
                // if unsubscribe has occurred there is no mandated behaviour in
                // terms of terminal events so we don't check them
            } else {
                sub.awaitTerminalEvent(c.waitForTerminalEventMs, TimeUnit.MILLISECONDS);
                if (c.expectError.isPresent()) {
                    sub.assertError(c.expectError.get());
                    // wait for more terminal events
                    pause(c.waitForMoreTerminalEventsMs, TimeUnit.MILLISECONDS);
                    if (sub.numOnCompletedEvents() > 0)
                        throw new UnexpectedOnCompletedException();
                } else {
                    sub.assertNoErrors();
                    // wait for more terminal events
                    pause(c.waitForMoreTerminalEventsMs, TimeUnit.MILLISECONDS);
                    if (sub.numOnCompletedEvents() > 1)
                        throw new TooManyOnCompletedException();
                    sub.assertNoErrors();
                }
            }

            if (c.expected.isPresent())
                sub.assertReceivedOnNext(c.expected.get(), c.ordered);
            if (c.expectSize.isPresent())
                sub.assertReceivedCountIs(c.expectSize.get());
            sub.assertUnsubscribed();
            if (c.checkSourceUnsubscribed)
                waitForUnsubscribe(sourceUnsubscribeLatch, c.waitForUnusbscribeMs,
                        TimeUnit.MILLISECONDS);
            if (c.expectedException.isPresent())
                throw new ExpectedExceptionNotThrownException();
        } catch (RuntimeException e) {
            if (!c.expectedException.isPresent() || !c.expectedException.get().isInstance(e))
                throw e;
            // otherwise was expected
        }
    }

    private static Action0 countDown(final CountDownLatch latch) {
        return new Action0() {
            @Override
            public void call() {
                latch.countDown();
            }
        };
    }

    private static <T> void waitForUnsubscribe(CountDownLatch latch, long duration, TimeUnit unit) {
        try {
            if (!latch.await(duration, unit))
                throw new UnsubscriptionFromSourceTimeoutException();
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static class UnsubscriptionFromSourceTimeoutException extends RuntimeException {
        private static final long serialVersionUID = -1142604414390722544L;
    }

    private static void pause(long duration, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(duration));
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    private static final class MyTestSubscriber<T> extends Subscriber<T> {

        private final List<T> next = new ArrayList<T>();
        private final Optional<Long> onStartRequest;
        private final Optional<Long> onNextRequest;
        private final Optional<Integer> unsubscribeAfter;
        private final CountDownLatch terminalLatch;
        private int completed = 0;
        private int count = 0;
        private int errors = 0;
        private final AtomicLong expected = new AtomicLong();
        private Optional<Throwable> lastError = Optional.absent();
        private Optional<Long> onNextRequest2;

        MyTestSubscriber(Optional<Integer> unsubscribeAfter, final Optional<Long> onStartRequest,
                final Optional<Long> onNextRequest, final Optional<Long> onNextRequest2) {
            this.unsubscribeAfter = unsubscribeAfter;
            this.onStartRequest = onStartRequest;
            this.onNextRequest = onNextRequest;
            this.onNextRequest2 = onNextRequest2;
            this.terminalLatch = new CountDownLatch(1);
        }

        MyTestSubscriber(Optional<Integer> unsubscribeAfter) {
            this(unsubscribeAfter, ABSENT, ABSENT, ABSENT);
        }

        @Override
        public void onStart() {
            if (!onStartRequest.isPresent())
                // if nothing requested in onStart then must be requesting all
                expected.set(Long.MAX_VALUE);
            else
                expected.set(0);
            if (onStartRequest.isPresent())
                requestMore(onStartRequest.get());
        }

        private void requestMore(long n) {
            if (expected.get() != Long.MAX_VALUE) {
                if (n > 0)
                    expected.addAndGet(n);
                // allow zero or negative requests to pass through as a test
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
            else {
                if (onNextRequest.isPresent())
                    requestMore(onNextRequest.get());
                if (onNextRequest2.isPresent())
                    requestMore(onNextRequest2.get());
            }
        }

        void assertError(Class<?> cls) {
            if (errors != 1 || !cls.isInstance(lastError.get()))
                throw new ExpectedErrorNotReceivedException();
        }

        void assertReceivedCountIs(long count) {
            if (count != next.size())
                throw new WrongOnNextCountException();
        }

        void awaitTerminalEvent(long duration, TimeUnit unit) {
            try {
                if (!terminalLatch.await(duration, unit))
                    throw new TerminalEventTimeoutException();
            } catch (InterruptedException e) {
                // do nothing
            }
        }

        void assertReceivedOnNext(List<T> expected, boolean ordered) {
            if (!TestingHelper.equals(expected, next, ordered))
                throw new UnexpectedOnNextException("expected=" + expected + ", actual=" + next);
        }

        void assertUnsubscribed() {
            if (!isUnsubscribed())
                throw new DownstreamUnsubscriptionDidNotOccurException();
        }

        int numOnCompletedEvents() {
            return completed;
        }

        void assertNoErrors() {
            if (errors > 0) {
                lastError.get().printStackTrace();
                throw new UnexpectedOnErrorException();
            }
        }

    }

    public static class TerminalEventTimeoutException extends RuntimeException {
        private static final long serialVersionUID = -7355281653999339840L;
    }

    public static class ExpectedErrorNotReceivedException extends RuntimeException {
        private static final long serialVersionUID = -567146145612029349L;
    }

    public static class ExpectedExceptionNotThrownException extends RuntimeException {
        private static final long serialVersionUID = -104410457605712970L;
    }

    public static class WrongOnNextCountException extends RuntimeException {
        private static final long serialVersionUID = 984672575527784559L;
    }

    public static class UnexpectedOnCompletedException extends RuntimeException {
        private static final long serialVersionUID = 7164517608988798969L;
    }

    public static class UnexpectedOnErrorException extends RuntimeException {
        private static final long serialVersionUID = -813740137771756205L;
    }

    public static class TooManyOnCompletedException extends RuntimeException {
        private static final long serialVersionUID = -405328882928962333L;
    }

    public static class DownstreamUnsubscriptionDidNotOccurException extends RuntimeException {
        private static final long serialVersionUID = 7218646111664183642L;
    }

    public static class UnexpectedOnNextException extends RuntimeException {
        private static final long serialVersionUID = -3656406263739222767L;

        public UnexpectedOnNextException(String message) {
            super(message);
        }

    }

    private static enum TestType {
        WITHOUT_BACKP, BACKP_INITIAL_REQUEST_MAX, BACKP_INITIAL_REQUEST_MAX_THEN_BY_ONE, BACKP_ONE_BY_ONE, BACKP_TWO_BY_TWO, BACKP_REQUEST_ZERO, BACKP_FIVE_BY_FIVE, BACKP_FIFTY_BY_FIFTY, BACKP_THOUSAND_BY_THOUSAND, BACKP_REQUEST_OVERFLOW;
    }

    private static <T> MyTestSubscriber<T> createTestSubscriber(Optional<Integer> unsubscribeAfter,
            long onStartRequest, Optional<Long> onNextRequest) {
        return new MyTestSubscriber<T>(unsubscribeAfter, of(onStartRequest), onNextRequest, ABSENT);
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
            return new MyTestSubscriber<T>(unsubscribeAfter, of(1L), of(0L), of(1L));
        else if (testType == TestType.BACKP_REQUEST_OVERFLOW)
            return new MyTestSubscriber<T>(unsubscribeAfter, of(1L), of(Long.MAX_VALUE / 3 * 2),
                    of(Long.MAX_VALUE / 3 * 2));
        else if (testType == TestType.BACKP_TWO_BY_TWO)
            return createTestSubscriberWithBackpNbyN(unsubscribeAfter, 2);
        else if (testType == TestType.BACKP_FIVE_BY_FIVE)
            return createTestSubscriberWithBackpNbyN(unsubscribeAfter, 5);
        else if (testType == TestType.BACKP_FIFTY_BY_FIFTY)
            return createTestSubscriberWithBackpNbyN(unsubscribeAfter, 50);
        else if (testType == TestType.BACKP_THOUSAND_BY_THOUSAND)
            return createTestSubscriberWithBackpNbyN(unsubscribeAfter, 1000);
        else
            throw new RuntimeException(testType + " not implemented");

    }

    private static <T> MyTestSubscriber<T> createTestSubscriberWithBackpNbyN(
            final Optional<Integer> unsubscribeAfter, final long requestSize) {
        return new MyTestSubscriber<T>(unsubscribeAfter, of(requestSize), ABSENT, of(requestSize));
    }

    @RunWith(Suite.class)
    @SuiteClasses({})
    private static class TestSuiteFromCases<T, R> extends TestSuite {

        TestSuiteFromCases(Class<?> cls, List<Case<T, R>> cases) {
            super(cls);
            for (Case<T, R> c : cases) {
                for (TestType testType : TestType.values())
                    if (testType != TestType.BACKP_REQUEST_OVERFLOW)
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
        if (a == null)
            return b == null;
        else if (b == null)
            return a == null;
        else if (a.size() != b.size())
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

    private static class TestingException extends RuntimeException {

        private static final long serialVersionUID = 4467514769366847747L;

    }

    /**
     * RuntimeException implementation to represent the situation of more items
     * being delivered by a source than are requested via backpressure.
     */
    public static class DeliveredMoreThanRequestedException extends RuntimeException {
        private static final long serialVersionUID = 1369440545774454215L;

        public DeliveredMoreThanRequestedException() {
            super("more items arrived than requested");
        }
    }

    /**
     * RuntimeException implementation to represent an assertion failure.
     */
    public static class AssertionException extends RuntimeException {
        private static final long serialVersionUID = -6846674323693517388L;

        public AssertionException(String message) {
            super(message);
        }

    }
}
