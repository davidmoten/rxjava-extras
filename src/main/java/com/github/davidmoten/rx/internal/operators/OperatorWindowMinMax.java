package com.github.davidmoten.rx.internal.operators;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import com.github.davidmoten.util.Preconditions;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;

/**
 * Uses a double-ended queue and collapses entries when they are redundant
 * (whenever a value is added to the queue all values at the end of the queue
 * that are greater or equal to that value are removed).
 * 
 * @param <T>
 *            generic type of stream emissions
 */
public final class OperatorWindowMinMax<T> implements Operator<T, T> {

    private final int windowSize;
    private final Comparator<? super T> comparator;
    private final Metric metric;

    public OperatorWindowMinMax(int windowSize, Comparator<? super T> comparator, Metric metric) {
        Preconditions.checkArgument(windowSize > 0, "windowSize must be greater than zero");
        Preconditions.checkNotNull(comparator, "comparator cannot be null");
        Preconditions.checkNotNull(metric, "metric cannot be null");
        this.windowSize = windowSize;
        this.comparator = comparator;
        this.metric = metric;
    }

    public enum Metric {
        MIN, MAX;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {

            long count = 0;

            // queue of indices
            final Deque<Long> q = new ArrayDeque<Long>();

            // map index to value
            final Map<Long, T> values = new HashMap<Long, T>();

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                count++;
                // add to queue
                addToQueue(t);
                if (count >= windowSize) {
                    // emit max

                    // head of queue is max
                    Long head = q.peekFirst();
                    final T value;
                    if (head == count - windowSize) {
                        // if window past that index then remove from map
                        values.remove(q.pollFirst());
                        value = values.get(q.peekFirst());
                    } else {
                        value = values.get(head);
                    }
                    child.onNext(value);
                }
            }

            private void addToQueue(T t) {
                Long v;
                while ((v = q.peekLast()) != null && compare(t, values.get(v)) <= 0) {
                    values.remove(q.pollLast());
                }
                values.put(count, t);
                q.offerLast(count);
            }

            @Override
            public void setProducer(final Producer producer) {
                child.setProducer(producer);
                producer.request(windowSize - 1);
            }

        };
    }

    private int compare(T a, T b) {
        if (metric == Metric.MIN) {
            return comparator.compare(a, b);
        } else {
            return comparator.compare(b, a);
        }
    }

}
