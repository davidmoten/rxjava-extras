package com.github.davidmoten.rx.internal.operators;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import rx.functions.Func0;

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
	private final Deque<Queue2<T>> queues = new ConcurrentLinkedDeque<Queue2<T>>();

	// guarded by `queues`
	private long count = 0;

	public RollingQueue(Func0<Queue2<T>> queueFactory, long maxItemsPerQueue) {
		this.queueFactory = queueFactory;
		this.maxItemsPerQueue = maxItemsPerQueue;
	}

	@Override
	public void close() {
		synchronized (queues) {
			while (!queues.isEmpty()) {
				Queue2<T> q = queues.pollFirst();
				q.dispose();
			}
		}
	}

	@Override
	public boolean offer(T t) {
		synchronized (queues) {
			count++;
			if (count == 1 || count == maxItemsPerQueue) {
				count = 1;
				queues.add(queueFactory.call());
			}
			return queues.peekLast().offer(t);
		}
	}

	@Override
	public T poll() {
		synchronized (queues) {
			if (queues.isEmpty())
				return null;
			else {
				while (true) {
					T value = queues.peekFirst().poll();
					if (value == null) {
						if (queues.size() <= 1) {
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
	}

	@Override
	public T peek() {
		synchronized (queues) {
			return queues.peekFirst().peek();
		}
	}

	@Override
	public boolean isEmpty() {
		synchronized (queues) {
			if (queues.isEmpty()) {
				return true;
			} else if (queues.size() == 1 && queues.peekFirst().isEmpty()) {
				return true;
			} else
				return false;
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