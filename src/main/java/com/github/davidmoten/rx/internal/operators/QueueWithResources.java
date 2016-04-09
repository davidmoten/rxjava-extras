package com.github.davidmoten.rx.internal.operators;

import java.util.Queue;

import rx.Subscription;

/**
 * A queue with associated underlying resources that can be freed, or closed
 * (via {@code unsubscribe}). An example of freeing resources would be to close
 * all open file system resources (like file descriptor handles) associated with
 * the queue and reopen them on next poll/offer that needs to access the file
 * system.
 *
 * @param <T>
 *            type of item on queue
 */
interface QueueWithResources<T> extends Queue<T>, Subscription {

	void freeResources();
}
