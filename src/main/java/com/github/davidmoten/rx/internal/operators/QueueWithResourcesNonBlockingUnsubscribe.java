package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps a Queue (like a file based queue) to provide concurrency guarantees
 * around calls to the close() method. Extends AtomicBoolean to save allocation.
 * The AtomicBoolean represents the closed status of the queue.
 * 
 * @param <T>
 *            type of item on queue
 */
final class QueueWithResourcesNonBlockingUnsubscribe<T> extends AbstractQueueWithResources<T> {

    private volatile boolean unsubscribing;

    // ensures queue.close() doesn't occur until outstanding peek(),offer(),
    // poll(), isEmpty() calls have finished. When currentCalls is zero a
    // close request can be actioned.
    private final AtomicInteger currentCalls = new AtomicInteger(0);

    private final AtomicBoolean unsubscribed;

    QueueWithResourcesNonBlockingUnsubscribe(QueueWithResources<T> queue) {
        super(queue);
        this.unsubscribing = false;
        this.unsubscribed = new AtomicBoolean(false);
    }

    @Override
    public T poll() {
        try {
            if (unsubscribing) {
                return null;
            } else {
                try {
                    currentCalls.incrementAndGet();
                    return super.poll();
                } finally {
                    currentCalls.decrementAndGet();
                }
            }
        } finally {
            checkUnsubscribe();
        }
    }

    @Override
    public boolean offer(T t) {
        try {
            if (unsubscribing) {
                return true;
            } else {
                try {
                    currentCalls.incrementAndGet();
                    return super.offer(t);
                } finally {
                    currentCalls.decrementAndGet();
                }
            }
        } finally {
            checkUnsubscribe();
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            if (unsubscribing) {
                return true;
            } else {
                try {
                    currentCalls.incrementAndGet();
                    return super.isEmpty();
                } finally {
                    currentCalls.decrementAndGet();
                }
            }
        } finally {
            checkUnsubscribe();
        }
   }

    @Override
    public void unsubscribe() {
        unsubscribing = true;
        checkUnsubscribe();
    }

    private void checkUnsubscribe() {
        if (unsubscribing && currentCalls.get() == 0 && unsubscribed.compareAndSet(false, true)) {
            super.unsubscribe();
        }
    }

    @Override
    public void freeResources() {
        super.freeResources();
    }

    @Override
    public boolean isUnsubscribed() {
        return unsubscribed.get();

    }

    @Override
    public long resourcesSize() {
        return super.resourcesSize();
    }
}