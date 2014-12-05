package com.github.davidmoten.rx;

import rx.Subscriber;

public interface Emitter<T, R> {

    void emitAll(T state, Subscriber<? super R> subscriber);

    void emitSome(LongWrapper numToEmit, T state, Subscriber<? super R> subscriber);

    boolean noMoreToEmit(T t);

}
