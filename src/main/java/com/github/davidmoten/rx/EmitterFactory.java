package com.github.davidmoten.rx;

import rx.Subscriber;

public interface EmitterFactory<T, R> {

    Emitter<T, R> create(T t, Subscriber<? super R> subscriber);

}
