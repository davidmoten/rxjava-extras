package com.github.davidmoten.rx.internal.operators;

import java.util.Queue;

import rx.Subscription;

interface CloseableQueue<T> extends Queue<T> {

	/**
	 * <p>
	 * Closes the queue (for example frees its resources by clearing its entries
	 * from memory or deleting its files from disk).
	 * 
	 * <p>
	 * Must be idempotent.
	 */
	void close();

}
