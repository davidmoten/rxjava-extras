package com.github.davidmoten.rx.internal.operators;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.github.davidmoten.rx.internal.operators.RollingQueue.Queue2;
import com.google.testing.threadtester.AnnotatedTestRunner;
import com.google.testing.threadtester.ThreadedAfter;
import com.google.testing.threadtester.ThreadedBefore;
import com.google.testing.threadtester.ThreadedMain;
import com.google.testing.threadtester.ThreadedSecondary;

import rx.functions.Func0;

public class RollingQueueTest {

	private volatile RollingQueue<Integer> q;

	@Test
	public void testConcurrently() {
		// Create an AnnotatedTestRunner that will run the threaded tests
		// defined in
		// this class. These tests are expected to makes calls to NameManager.
		AnnotatedTestRunner runner = new AnnotatedTestRunner();
		runner.runTests(this.getClass(), RollingQueue.class);
	}

	@ThreadedBefore
	public void before() {
		// Set up a new NameManager instance for the test
		q = new RollingQueue<Integer>(queueFactory, 3);
	}


	@ThreadedMain
	public void main() {
		q.offer(1);
		q.offer(2);
		q.offer(3);
		q.offer(4);
		q.offer(5);
		q.offer(6);
		System.out.println("offered");
	}

	@ThreadedSecondary
	public void secondary() {
		System.out.println(q.poll());
		System.out.println(q.poll());
		System.out.println(q.poll());
		System.out.println(q.poll());
		System.out.println(q.poll());
		System.out.println(q.poll());
		System.out.println("----");
	}

	@ThreadedAfter
	public void after() {
	}
	
	private static final Func0<Queue2<Integer>> queueFactory = new Func0<Queue2<Integer>>() {

		@Override
		public Queue2<Integer> call() {
			return new Queue2<Integer>() {

				final Queue<Integer> queue = new LinkedBlockingDeque<Integer>();
				final AtomicBoolean closed = new AtomicBoolean(false);

				@Override
				public Integer peek() {
					if (closed.get()) {
						return null;
					} else {
						return queue.peek();
					}
				}

				@Override
				public Integer poll() {
					if (closed.get()) {
						return null;
					} else {
						return queue.poll();
					}
				}

				@Override
				public boolean offer(Integer t) {
					if (closed.get()) {
						return true;
					} else {
						return queue.offer(t);
					}
				}

				@Override
				public void dispose() {
					if (closed.compareAndSet(false, true)) {
						queue.clear();
					}
				}

				@Override
				public boolean isEmpty() {
					return queue.isEmpty();
				}
			};
		}
	};

}
