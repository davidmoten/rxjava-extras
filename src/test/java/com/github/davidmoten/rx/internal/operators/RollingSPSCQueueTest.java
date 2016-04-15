package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Test;

import com.google.testing.threadtester.AnnotatedTestRunner;
import com.google.testing.threadtester.MethodOption;
import com.google.testing.threadtester.ThreadedAfter;
import com.google.testing.threadtester.ThreadedBefore;
import com.google.testing.threadtester.ThreadedMain;
import com.google.testing.threadtester.ThreadedSecondary;

import rx.functions.Func0;

/**
 * Uses thread-weaver to test concurrent calls of poll and offer using
 * interleaving. Not sure if is comprehensive!
 */
public class RollingSPSCQueueTest {

	private volatile RollingSPSCQueue<Integer> q;

	@Ignore
	@Test
	public void testPushAndPollUsingThreadWeaver() {
		// Create an AnnotatedTestRunner that wisll run the threaded tests
		// defined in this class.
		AnnotatedTestRunner runner = new AnnotatedTestRunner();
		HashSet<String> methods = new HashSet<String>();
		runner.setMethodOption(MethodOption.ALL_METHODS, methods);
		runner.setDebug(true);
		runner.runTests(this.getClass(), RollingSPSCQueue.class);
	}

	@ThreadedBefore
	public void before() {
		q = new RollingSPSCQueue<Integer>(queueFactory,0, 3);
		q.offer(1);
	}

	@ThreadedMain
	public void main() {
		q.offer(2);
	}

	@ThreadedSecondary
	public void secondary() {
		q.poll();
	}

	@ThreadedAfter
	public void after() {
		Integer first = q.poll();
		Integer second = q.poll();
		assertTrue(first == 1 && second == null || first == 2 && second == null);
	}

	private static final Func0<QueueWithResources<Integer>> queueFactory = new Func0<QueueWithResources<Integer>>() {

		@Override
		public QueueWithResources<Integer> call() {
			return new QueueWithResourcesForwarder<Integer>(new LinkedBlockingDeque<Integer>()) {

				final AtomicBoolean closed = new AtomicBoolean(false);

				@Override
				public Integer poll() {
					if (closed.get()) {
						return null;
					} else {
						return super.poll();
					}
				}

				@Override
				public boolean offer(Integer t) {
					if (closed.get()) {
						return true;
					} else {
						return super.offer(t);
					}
				}

				@Override
				public void unsubscribe() {
					if (closed.compareAndSet(false, true)) {
						super.clear();
					}
				}
			};
		}
	};

}
