package com.github.davidmoten.rx.internal.operators;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.davidmoten.util.Preconditions;

import rx.Subscription;
import rx.functions.Func0;
import rx.plugins.RxJavaPlugins;

/**
 * <p>
 * This abstraction around multiple queues exists as a strategy to reclaim file
 * system space taken by MapDB databases. The strategy is to use a double ended
 * queue of queues (each queue in a separate database). As the number of entries
 * added to a queue (regardless of how many are read) meets a threshold another
 * queue is created on the end of the deque and new entries then are added to
 * that. As entries are read from a queue that is not the last queue, it is
 * deleted when empty and its file resources recovered (deleted).
 * 
 * <p>
 * MapDB has the facility to reuse space (at significant speed cost) but to
 * shrink allocated space (due to a surge in queue size) requires a non-trivial
 * blocking operation ({@code DB.compact()}) so it seems better to avoid
 * blocking and incur regular small new DB instance creation costs (or even
 * create the next queue in advance on another thread).
 * 
 * <p>
 * {@code RollingQueue} is partially thread-safe. It is designed to support
 * {@code OperatorBufferToFile} and expects calls to {@code offer()} to be
 * sequential (a happens-before relationship), and calls to {@code poll()} to be
 * sequential. Calls to {@code offer()}, {@code poll()}, {@code isEmpty()},
 * {@code peek()},{@code close()} may happen concurrently.
 * 
 * @param <T>
 *            type of item being queued
 */
final class RollingQueue<T> extends AtomicBoolean implements CloseableQueue<T> {

	// inherited boolean represents the closed status of the RollingQueue

	private static final long serialVersionUID = 6212213475110919831L;

	interface Queue2<T> {

		// returns null if closed
		T poll();

		// returns true if closed
		boolean offer(T t);

		void close();

		// returns true if closed
		boolean isEmpty();
	}

	private final Func0<Queue2<T>> queueFactory;
	private final long maxItemsPerQueue;
	private final Deque<Queue2<T>> queues = new LinkedBlockingDeque<Queue2<T>>();

	// counter used to determine when to rollover to another queue
	// visibility managed by the fact that calls to offer are happens-before
	// sequential
	private long count;

	RollingQueue(Func0<Queue2<T>> queueFactory, long maxItemsPerQueue) {
		Preconditions.checkNotNull(queueFactory);
		Preconditions.checkArgument(maxItemsPerQueue > 1, "maxItemsPerQueue must be > 1");
		this.queueFactory = queueFactory;
		this.maxItemsPerQueue = maxItemsPerQueue;
		this.count = 0;
		// store-store barrier
		lazySet(false);
	}

	@Override
	public void unsubscribe() {
		if (compareAndSet(false, true)) {
			try {
				// thread-safe and idempotent
				for (Queue2<T> q : queues) {
					q.close();
				}
			} catch (RuntimeException e) {
				RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
				throw e;
			} catch (Error e) {
				RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
				throw e;
			}
			// Would be nice to clear `queues` at this point to release Queue2
			// references for gc early but would have to wait for an outstanding
			// offer/poll/peek/isEmpty. This could make things a bit more
			// complex and add overhead. Note that Queue2 instances after
			// closing release their references to DB and Queue instances so
			// going further to release Queue2 objects themselves is not really
			// worth it.
		}
	}

	@Override
	public boolean isUnsubscribed() {
		return get();
	}

	@Override
	public boolean offer(T t) {
		// limited thread safety (offer/poll/close/peek/isEmpty concurrent but
		// not offer and offer)
		if (get()) {
			return true;
		} else {
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
		// limited thread safety (offer/poll/close/peek/isEmpty concurrent but
		// not poll and poll)
		if (get()) {
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
						// don't have concurrent poll/poll so don't have to
						// do null check on removed
						removed.close();
					}
				} else {
					return value;
				}
			}
		}
	}

	@Override
	public boolean isEmpty() {
		// thread-safe (will just return true if queue has been closed)
		if (get()) {
			return true;
		} else {
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
	public T peek() {
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

	@SuppressWarnings("hiding")
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