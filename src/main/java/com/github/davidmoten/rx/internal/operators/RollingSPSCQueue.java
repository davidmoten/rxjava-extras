package com.github.davidmoten.rx.internal.operators;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import com.github.davidmoten.util.Preconditions;

import rx.functions.Func0;
import rx.plugins.RxJavaPlugins;

/**
 * <p>
 * This abstraction around multiple queues exists as a strategy to reclaim file
 * system space taken by file based queues. The strategy is to use a double
 * ended queue of queues (each queue having its own files). As the number of
 * entries added to a queue (regardless of how many are read) meets a threshold
 * another queue is created on the end of the deque and new entries then are
 * added to that. As entries are read from a queue that is not the last queue,
 * it is deleted when empty and its file resources recovered (deleted).
 * 
 * <p>
 * {@code RollingSPSCQueue} is partially thread-safe. It is designed to support
 * {@code OperatorBufferToFile} and expects calls to {@code offer()} to be
 * sequential (a happens-before relationship), and calls to {@code poll()} to be
 * sequential. Calls to {@code offer()}, {@code poll()}, {@code isEmpty()},
 * {@code peek()},{@code close()} may happen concurrently.
 * 
 * @param <T>
 *            type of item being queued
 */
class RollingSPSCQueue<T> implements QueueWithResources<T> {

	private final Func0<QueueWithResources<T>> queueFactory;
	private final long maxSizeBytesPerQueue;
	private final long maxItemsPerQueue;
	private final Deque<QueueWithResources<T>> queues = new LinkedList<QueueWithResources<T>>();

	// counter used to determine when to rollover to another queue
	// visibility managed by the fact that calls to offer are happens-before
	// sequential
	private long count;

	// guarded by queues
	private boolean unsubscribed;

	RollingSPSCQueue(Func0<QueueWithResources<T>> queueFactory, long maxSizeBytesPerQueue, long maxItemsPerQueue) {
		Preconditions.checkNotNull(queueFactory);
		Preconditions.checkArgument(maxSizeBytesPerQueue > 0, "maxSizeBytesPerQueue must be greater than zero");
		Preconditions.checkArgument(maxItemsPerQueue > 1, "maxSizeBytesPerQueue must be greater than one");
		this.count = 0;
		this.maxSizeBytesPerQueue = maxSizeBytesPerQueue;
		this.unsubscribed = false;
		this.queueFactory = queueFactory;
		this.maxItemsPerQueue = maxItemsPerQueue;
	}

	@Override
	public void unsubscribe() {
		if (unsubscribed) {
			return;
		}
		synchronized (queues) {
			if (!unsubscribed) {
				unsubscribed = true;
				try {
					for (QueueWithResources<T> q : queues) {
						q.unsubscribe();
					}
					queues.clear();
				} catch (RuntimeException e) {
					RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
					throw e;
				} catch (Error e) {
					RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
					throw e;
				}
			}
		}
	}

	@Override
	public boolean isUnsubscribed() {
		if (unsubscribed)
			return true;
		synchronized (queues) {
			return unsubscribed;
		}
	}

	@Override
	public boolean offer(T t) {
		// limited thread safety (offer/poll/close/peek/isEmpty concurrent but
		// not offer and offer)
		if (unsubscribed) {
			return true;
		}
		count++;
		if (createAnotherQueue()) {
			count = 1;
			QueueWithResources<T> q = queueFactory.call();
			synchronized (queues) {
				if (!unsubscribed) {
					QueueWithResources<T> last = queues.peekLast();
					if (last != null) {
						last.freeResources();
					}
					queues.offerLast(q);
					return q.offer(t);
				} else {
					return true;
				}
			}
		} else {
			synchronized (queues) {
				if (unsubscribed) {
					return true;
				}
				return queues.peekLast().offer(t);
			}
		}
	}

	private boolean createAnotherQueue() {
		if (count == 1) {
			// first call to offer
			return true;
		} else if (count == maxItemsPerQueue) {
			return true;
		} else if (maxSizeBytesPerQueue != Long.MAX_VALUE) {
			synchronized (queues) {
				if (unsubscribed) {
					return true;
				}
				return queues.peekLast().resourcesSize() >= maxSizeBytesPerQueue;
			}
		} else {
			return false;
		}
	}

	@Override
	public T poll() {
		// limited thread safety (offer/poll/close/peek/isEmpty concurrent but
		// not poll and poll)
		if (unsubscribed) {
			return null;
		}
		while (true) {
			synchronized (queues) {
				if (unsubscribed) {
					return null;
				}
				QueueWithResources<T> first = queues.peekFirst();
				if (first == null) {
					return null;
				}
				T value = first.poll();
				if (value == null) {
					if (first == queues.peekLast()) {
						return null;
					} else {
						QueueWithResources<T> removed = queues.pollFirst();
						if (removed != null)
							removed.unsubscribe();
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
		if (unsubscribed) {
			return true;
		}
		synchronized (queues) {
			if (unsubscribed) {
				return true;
			}
			QueueWithResources<T> first = queues.peekFirst();
			if (first == null) {
				return true;
			} else {
				return queues.peekLast() == first && first.isEmpty();
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

	@Override
	public void freeResources() {
		// do nothing
	}

	@Override
	public long resourcesSize() {
		throw new UnsupportedOperationException();
	}

}