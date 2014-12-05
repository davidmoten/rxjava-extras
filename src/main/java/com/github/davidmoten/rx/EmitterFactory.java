package com.github.davidmoten.rx;

import rx.Subscriber;

public interface EmitterFactory<T> {

    Emitter create(Subscriber<? super T> subscriber);

}
