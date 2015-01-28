package com.github.davidmoten.rx.testing;

import static java.util.Arrays.asList;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class TestingHelperMergeTest extends TestCase {

	private static final Observable<Integer> MERGE_WITH = Observable
			.from(asList(7, 8, 9));

	public static TestSuite suite() {
		TestingHelper.includeBackpressureRequestOverflowTest = false;
		return TestingHelper
				.function(merge)
				.waitForUnsubscribe(100, TimeUnit.MILLISECONDS)
				.waitForTerminalEvent(10, TimeUnit.SECONDS)
				.waitForMoreTerminalEvents(100, TimeUnit.MILLISECONDS)
				// test empty
				.name("testEmptyWithOtherReturnsOther")
				.fromEmpty()
				.expect(7, 8, 9)
				// test error
				.name("testMergeErrorReturnsError")
				.fromError()
				.expectError()
				// test error after items
				.name("testMergeErrorAfter2ReturnsError")
				.fromErrorAfter(1, 2)
				.expectError()
				// test non-empty count
				.name("testTwoWithOtherReturnsTwoAndOtherInAnyOrder")
				.from(1, 2)
				.expectAnyOrder(1, 7, 8, 9, 2)
				// test single input
				.name("testOneWithOtherReturnsOneAndOtherInAnyOrder").from(1)
				.expectAnyOrder(7, 1, 8, 9)
				// unsub before completion
				.name("testTwoWithOtherUnsubscribedAfterOneReturnsOneItemOnly")
				.from(1, 2).unsubscribeAfter(1).expectSize(1)
				// get test suites
				.testSuite(TestingHelperMergeTest.class);
	}

	public void testDummy() {
		// just here to fool eclipse
	}

	private static final Func1<Observable<Integer>, Observable<Integer>> merge = new Func1<Observable<Integer>, Observable<Integer>>() {
		@Override
		public Observable<Integer> call(Observable<Integer> o) {
			return o.mergeWith(MERGE_WITH.subscribeOn(Schedulers.computation()))
					.subscribeOn(Schedulers.computation());
		}
	};

}
