package com.github.davidmoten.rx.internal.operators;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import rx.functions.Func0;

/**
 * This abstraction around multiple queues exists because to reclaim file system
 * space taken by MapDB databases the selected strategy is to use a double ended
 * queue of queue (each queue in a separate database). As the number of entries
 * added to a queue (regardless of how many are read) meets a threshold another
 * queue is created on the end of the deque and new entries then are added to
 * that. As entries are read from a queue that is not the last queue, it is
 * deleted when empty and its file resources recovered (deleted).
 * 
 * MapDB has the facility to reuse space (at significant speed cost) but to
 * shrink allocated space (due to a surge in queue size) requires a non-trivial
 * blocking operation ({@code DB.compact()}) so it seems better to avoid
 * blocking and incur regular small new DB instance creation costs.
 * 
 * @param <T>
 *            type of item being queued
 */
public final class RollingQueue<T> implements CloseableQueue<T> {

	public interface Queue2<T> {
		T peek();

		T poll();

		boolean offer(T t);

		void dispose();

		boolean isEmpty();
	}

	private final Func0<Queue2<T>> queueFactory;
	private final long maxItemsPerQueue;
	private final Deque<Queue2<T>> queues = new LinkedBlockingDeque<Queue2<T>>();
	private final AtomicBoolean closed = new AtomicBoolean(false);

	private final AtomicLong count = new AtomicLong(0);

	public RollingQueue(Func0<Queue2<T>> queueFactory, long maxItemsPerQueue) {
		this.queueFactory = queueFactory;
		this.maxItemsPerQueue = maxItemsPerQueue;
	}

	@Override
	public void close() {
		if (closed.compareAndSet(false, true)) {
			// thread-safe and idempotent
			for (Queue2<T> q : queues) {
				q.dispose();
			}
		}
	}

	@Override
	public boolean offer(T t) {
		// limited thread safety (offer and poll concurrent but not offer
		// and offer)
		if (closed.get()) {
			return true;
		}
		long c = count.incrementAndGet();
		if (c == 1 || c == maxItemsPerQueue) {
			count.set(1);
			queues.add(queueFactory.call());
		}
		return queues.peekLast().offer(t);
	}

	@Override
	public T poll() {
		// limited thread safety (offer and poll concurrent but not poll
		// and poll)
		if (closed.get()) {
			return null;
		} else if (queues.isEmpty())
			return null;
		else {
			while (true) {
				Queue2<T> first = queues.peekFirst();
				T value = first.poll();
				if (value == null) {
					if (first == queues.peekLast()) {
						return null;
					} else {
						Queue2<T> removed = queues.pollFirst();
						removed.dispose();
					}
				} else {
					return value;
				}
			}
		}
	}

	@Override
	public T peek() {
		// thread-safe
		if (closed.get()) {
			return null;
		} else {
			return queues.peekFirst().peek();
		}
	}

	@Override
	public boolean isEmpty() {
		if (closed.get()) {
			return true;
		} else {
			// thread-safe
			Queue2<T> first = queues.peekFirst();
			if (first == null) {
				return true;
			} else if (queues.peekLast() == first && first.isEmpty()) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<T> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(T e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T element() {
		throw new UnsupportedOperationException();
	}

}