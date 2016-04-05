package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.github.davidmoten.rx.buffertofile.DataSerializers;

public class PersistentSPSCQueueTest {

	@Test
	public void test() {
		PersistentSPSCQueue<Integer> q = createQueue();
		q.offer(1);
		assertEquals(1, (int) q.poll());
		assertNull(q.poll());
	}

	@Test
	public void test2() {
		PersistentSPSCQueue<Integer> q = createQueue();
		q.offer(1);
		q.offer(2);
		assertEquals(1, (int) q.poll());
		assertEquals(2, (int) q.poll());
		assertNull(q.poll());
	}

	@Test
	public void test3() {
		PersistentSPSCQueue<Integer> q = createQueue();
		assertNull(q.poll());
		q.offer(1);
		q.offer(2);
		assertEquals(1, (int) q.poll());
		q.offer(3);
		assertEquals(2, (int) q.poll());
		assertEquals(3, (int) q.poll());
		assertNull(q.poll());
	}

	@Test
	public void testConcurrent() throws InterruptedException, ExecutionException {
		File file = new File("target/pq2");
		file.delete();
		final PersistentSPSCQueue<Integer> queue = new PersistentSPSCQueue<Integer>(1024, file, DataSerializers.integer());
		final int max = 10000000;
		long t = System.currentTimeMillis();
		final AtomicBoolean failed = new AtomicBoolean(false);
		Thread t1 = new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 1; i <= max; i++) {
					queue.offer(i);
				}
			}
		});
		Thread t2 = new Thread(new Runnable() {

			@Override
			public void run() {
				int i = 1;
				while (i <= max) {
					Integer t = queue.poll();
					if (t != null) {
						if (i != t) {
							failed.set(true);
							System.out.println("failed for i = " + i);
						}
						i++;
					} 
				}
			}
		});
		t1.start();
		t2.start();
		t1.join();
		t2.join();
		System.out.println(Math.round(max * 1000.0 / (System.currentTimeMillis() - t)) + " per second");
		assertFalse(failed.get());
	}

	private static PersistentSPSCQueue<Integer> createQueue() {
		File file = new File("target/pq");
		file.delete();
		PersistentSPSCQueue<Integer> q = new PersistentSPSCQueue<Integer>(5, file, DataSerializers.integer());
		return q;
	}

}
