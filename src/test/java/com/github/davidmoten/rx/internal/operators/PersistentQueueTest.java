package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.github.davidmoten.rx.buffertofile.DataSerializers;

public class PersistentQueueTest {

	@Test
	public void test() {
		PersistentQueue<Integer> q = createQueue();
		q.offer(1);
		assertEquals(1, (int) q.poll());
		assertNull(q.poll());
	}

	@Test
	public void test2() {
		PersistentQueue<Integer> q = createQueue();
		q.offer(1);
		q.offer(2);
		assertEquals(1, (int) q.poll());
		assertEquals(2, (int) q.poll());
		assertNull(q.poll());
	}

	@Test
	public void test3() {
		PersistentQueue<Integer> q = createQueue();
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
		final PersistentQueue<Integer> queue = new PersistentQueue<Integer>(10, file, DataSerializers.integer());
		final int max = 10000000;
		long t = System.currentTimeMillis();
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
						assertEquals(i, (int) t);
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
	}

	private static PersistentQueue<Integer> createQueue() {
		File file = new File("target/pq");
		file.delete();
		PersistentQueue<Integer> q = new PersistentQueue<Integer>(5, file, DataSerializers.integer());
		return q;
	}

}
