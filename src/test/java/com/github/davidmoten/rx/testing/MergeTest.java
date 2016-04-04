package com.github.davidmoten.rx.testing;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class MergeTest {

	@Test
	public void testMerge() throws InterruptedException {
		// while (true) {
		final CountDownLatch latch = new CountDownLatch(1);
		final List<Integer> list = new ArrayList<Integer>();
		Subscriber<Integer> sub = new Subscriber<Integer>() {

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

			}

			@Override
			public void onNext(Integer t) {
				list.add(t);
				request(0);
				request(1);
			}
		};
		Observable.<Integer> empty()
				.mergeWith(Observable.from(Arrays.asList(1, 2, 3)).subscribeOn(Schedulers.computation()))
				.subscribe(sub);
		if (!latch.await(1, TimeUnit.SECONDS))
			throw new RuntimeException("timed out");
		assertEquals(Arrays.asList(1, 2, 3), list);
		// }
	}

}
