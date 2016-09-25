package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static rx.Observable.from;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.testing.TestingHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OrderedMergeTest {

	private static final Comparator<Integer> comparator = new Comparator<Integer>() {
		@Override
		public int compare(Integer a, Integer b) {
			return a.compareTo(b);
		}
	};

	@Test
	public void testMerge() {
		// hang on to one stand-alone test like this so we can customize for
		// failure cases arriving out of testWithAllCombinationsFromPowerSet
		Observable<Integer> o1 = Observable.just(1, 2, 4, 10);
		Observable<Integer> o2 = Observable.just(3, 5, 11);
		check(o1, o2, 1, 2, 3, 4, 5, 10, 11);
	}

	@Test
	public void testWithAllCombinationsFromPowerSet() {
		checkAllCombinationsFromPowerSet(Schedulers.immediate());
	}

	@Test
	public void testWithAllCombinationsFromPowerSetAsync() {
		long t = System.currentTimeMillis();
		while (System.currentTimeMillis() - t <= TimeUnit.SECONDS.toMillis(5)) {
			checkAllCombinationsFromPowerSet(Schedulers.computation());
		}
	}

	private void checkAllCombinationsFromPowerSet(Scheduler scheduler) {
		// this test covers everything!
		for (int n = 0; n <= 10; n++) {
			Set<Integer> numbers = Sets.newTreeSet();
			for (int i = 1; i <= n; i++) {
				numbers.add(i);
			}
			for (Set<Integer> a : Sets.powerSet(numbers)) {
				TreeSet<Integer> x = Sets.newTreeSet(a);
				TreeSet<Integer> y = Sets.newTreeSet(Sets.difference(numbers, x));
				Observable<Integer> o1 = from(x).subscribeOn(scheduler);
				Observable<Integer> o2 = from(y).subscribeOn(scheduler);
				List<Integer> list = o1.compose(Transformers.orderedMergeWith(o2, comparator)).toList().toBlocking()
						.single();
				// System.out.println(x + " " + y);
				assertEquals(Lists.newArrayList(numbers), list);
			}
		}
	}

	private static void check(Observable<Integer> o1, Observable<Integer> o2, Integer... values) {
		List<Integer> list = o1.compose(Transformers.orderedMergeWith(o2, comparator)).toList().toBlocking().single();
		assertEquals(Arrays.asList(values), list);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSymmetricMerge() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
		Observable<Integer> o2 = Observable.just(2, 4, 6, 8);

		TestSubscriber<Integer> ts = TestSubscriber.create();
		OrderedMerge.create(Arrays.asList(o1, o2)).subscribe(ts);
		ts.assertNoErrors();
		ts.assertCompleted();
		ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAsymmetricMerge() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
		Observable<Integer> o2 = Observable.just(2, 4);

		TestSubscriber<Integer> ts = TestSubscriber.create();
		OrderedMerge.create(Arrays.asList(o1, o2)).subscribe(ts);

		ts.assertNoErrors();
		ts.assertCompleted();
		ts.assertValues(1, 2, 3, 4, 5, 7);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSymmetricMergeAsync() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7).observeOn(Schedulers.computation());
		Observable<Integer> o2 = Observable.just(2, 4, 6, 8).observeOn(Schedulers.computation());

		OrderedMerge.<Integer> create(Arrays.asList(o1, o2)) //
		    .to(TestingHelper.<Integer>test())
		    .awaitTerminalEvent(1, TimeUnit.SECONDS) //
		    .assertNoErrors() //
		    .assertCompleted() //
		    .assertValues(1, 2, 3, 4, 5, 6, 7, 8);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAsymmetricMergeAsync() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7).observeOn(Schedulers.computation());
		Observable<Integer> o2 = Observable.just(2, 4).observeOn(Schedulers.computation());

		TestSubscriber<Integer> ts = TestSubscriber.create();
		OrderedMerge.create(Arrays.asList(o1, o2)).subscribe(ts);

		ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
		ts.assertNoErrors();
		ts.assertCompleted();
		ts.assertValues(1, 2, 3, 4, 5, 7);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEmptyEmpty() {
		Observable<Integer> o1 = Observable.empty();
		Observable<Integer> o2 = Observable.empty();

		TestSubscriber<Integer> ts = TestSubscriber.create();
		OrderedMerge.create(Arrays.asList(o1, o2)).subscribe(ts);

		ts.assertNoErrors();
		ts.assertCompleted();
		ts.assertNoValues();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEmptySomething1() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
		Observable<Integer> o2 = Observable.empty();

		TestSubscriber<Integer> ts = TestSubscriber.create();
		OrderedMerge.create(Arrays.asList(o1, o2)).subscribe(ts);

		ts.assertNoErrors();
		ts.assertCompleted();
		ts.assertValues(1, 3, 5, 7);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEmptySomething2() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
		Observable<Integer> o2 = Observable.empty();

		TestSubscriber<Integer> ts = TestSubscriber.create();
		OrderedMerge.create(Arrays.asList(o2, o1)).subscribe(ts);

		ts.assertNoErrors();
		ts.assertCompleted();
		ts.assertValues(1, 3, 5, 7);
	}

	private static class TestException extends Exception {

		private static final long serialVersionUID = -1204531902391807941L;

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testErrorInMiddle() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
		Observable<Integer> o2 = Observable.just(2).concatWith(Observable.<Integer> error(new TestException()));

		TestSubscriber<Integer> ts = TestSubscriber.create();
		OrderedMerge.create(Arrays.asList(o1, o2)).subscribe(ts);

		ts.assertError(TestException.class);
		ts.assertNotCompleted();
		ts.assertValues(1, 2);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testErrorImmediately() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
		Observable<Integer> o2 = Observable.<Integer> error(new TestException());

		TestSubscriber<Integer> ts = TestSubscriber.create();
		OrderedMerge.create(Arrays.asList(o1, o2)).subscribe(ts);

		ts.assertError(TestException.class);
		ts.assertNotCompleted();
		ts.assertNoValues();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testErrorInMiddleDelayed() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
		Observable<Integer> o2 = Observable.just(2).concatWith(Observable.<Integer> error(new TestException()));

		TestSubscriber<Integer> ts = TestSubscriber.create();
		OrderedMerge.create(Arrays.asList(o1, o2), true).subscribe(ts);

		ts.assertError(TestException.class);
		ts.assertValues(1, 2, 3, 5, 7);
		ts.assertNotCompleted();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testErrorImmediatelyDelayed() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
		Observable<Integer> o2 = Observable.<Integer> error(new TestException());

		TestSubscriber<Integer> ts = TestSubscriber.create();
		OrderedMerge.create(Arrays.asList(o1, o2), true).subscribe(ts);

		ts.assertError(TestException.class);
		ts.assertNotCompleted();
		ts.assertValues(1, 3, 5, 7);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTake() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
		Observable<Integer> o2 = Observable.just(2, 4, 6, 8);

		TestSubscriber<Integer> ts = TestSubscriber.create();
		OrderedMerge.create(Arrays.asList(o1, o2)).take(2).subscribe(ts);

		ts.assertNoErrors();
		ts.assertCompleted();
		ts.assertValues(1, 2);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testBackpressure() {
		Observable<Integer> o1 = Observable.just(1, 3, 5, 7);
		Observable<Integer> o2 = Observable.just(2, 4, 6, 8);

		TestSubscriber<Integer> ts = TestSubscriber.create(0);
		OrderedMerge.create(Arrays.asList(o1, o2)).subscribe(ts);

		ts.requestMore(2);

		ts.assertNoErrors();
		ts.assertNotCompleted();
		ts.assertValues(1, 2);

	}
}