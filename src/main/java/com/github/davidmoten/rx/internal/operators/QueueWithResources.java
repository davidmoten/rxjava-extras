package com.github.davidmoten.rx.internal.operators;

/**
 * <p>
 * A queue with associated underlying resources that can be freed, or closed
 * (via {@code unsubscribe}).
 * 
 * <p>
 * An example of freeing resources would be to close all open file system
 * resources (like file descriptor handles) associated with the queue and reopen
 * them on next poll/offer that needs to access the file system. This would
 * avoid running out of file descriptors in some situations.
 *
 * @param <T>
 *            type of item on queue
 */
interface QueueWithResources<T> extends QueueWithSubscription<T> {

	/**
	 * <p>
	 * Frees resources associated with this queue. This is not for closing a
	 * queue but rather in the situation where the queue is not used for a
	 * period of time then it may be desirable to reduce its resource usage
	 * (without compromising its content).
	 * 
	 * <p>
	 * An example of freeing resources would be to close all open file system
	 * resources (like file descriptor handles) associated with the queue and
	 * reopen them on next poll/offer that needs to access the file system. This
	 * would avoid running out of file descriptors in some situations.
	 */
	void freeResources();

	long resourcesSize();
}
