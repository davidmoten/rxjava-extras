package com.github.davidmoten.rx.internal.operators;

import java.util.Queue;

import rx.Subscription;

interface CloseableQueue<T> extends Queue<T>, Subscription {

    void setReadOnly();
}
