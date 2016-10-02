package com.github.davidmoten.rx.testing;

import com.github.davidmoten.rx.Transformers;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

public class TestingHelperMapLastTest extends TestCase {
	private static final Func1<Observable<Integer>, Observable<Integer>> MAP_LAST = new Func1<Observable<Integer>, Observable<Integer>>() {
		@Override
		public Observable<Integer> call(Observable<Integer> o) {
			return o.compose(Transformers.mapLast(new Func1<Integer, Integer>() {
				@Override
				public Integer call(Integer x) {
					return x + 1;
				}
			}));
		}
	};

	public static TestSuite suite() {
		return TestingHelper.function(MAP_LAST) //
				.name("testMapLastOfEmptyReturnsEmpty").fromEmpty().expectEmpty() //
				.name("testMapLastOfOne").from(1).expect(2) //
				.name("testMapLastOfTwo").from(1, 2).expect(1, 3) //
				.name("testMapLastOfEmptyThenError").fromError().expectError() //
				// get test suites
				.testSuite(TestingHelperMapLastTest.class);
	}

	public void testDummy() {
		// just here to fool eclipse
	}

}