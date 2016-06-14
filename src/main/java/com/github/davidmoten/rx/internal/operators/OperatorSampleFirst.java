package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.TimeUnit;

import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;

/**
 * Throttle by windowing a stream and returning the first value in each window.
 * @param <T> the value type
 */
public final class OperatorSampleFirst<T> implements Operator<T, T> {

    private final long windowDurationMs;
    private final Scheduler scheduler;

    private static long UNSET = Long.MIN_VALUE;

    public OperatorSampleFirst(long windowDurationMs, TimeUnit unit, Scheduler scheduler) {
        this.windowDurationMs = unit.toMillis(windowDurationMs);
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {

            private long nextWindowStartTime = UNSET;

            @Override
            public void onStart() {
                request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T t) {
                long now = scheduler.now();
                if (nextWindowStartTime == UNSET) {
                    nextWindowStartTime = now + windowDurationMs;
                    subscriber.onNext(t);
                } else if (now >= nextWindowStartTime) {
                    // ensure that we advance the next window start time to just
                    // beyond now
                    long n = (now - nextWindowStartTime) / windowDurationMs + 1;
                    nextWindowStartTime += n * windowDurationMs;
                    subscriber.onNext(t);
                }
            }

            @Override
            public void onCompleted() {
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

        };
    }
}
