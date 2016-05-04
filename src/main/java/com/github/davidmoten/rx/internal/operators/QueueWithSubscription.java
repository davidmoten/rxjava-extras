package com.github.davidmoten.rx.internal.operators;

import java.util.Queue;

import rx.Subscription;

public interface QueueWithSubscription<T> extends Queue<T>, Subscription {

}
