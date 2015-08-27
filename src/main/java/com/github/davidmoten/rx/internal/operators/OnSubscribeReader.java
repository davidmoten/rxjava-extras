package com.github.davidmoten.rx.internal.operators;

import java.io.IOException;
import java.io.Reader;

import rx.Subscriber;
import rx.observables.AbstractOnSubscribe;

public final class OnSubscribeReader extends AbstractOnSubscribe<String, Reader> {

    private final Reader reader;
    private final int size;

    public OnSubscribeReader(Reader reader, int size) {
        this.reader = reader;
        this.size = size;
    }

    @Override
    protected Reader onSubscribe(Subscriber<? super String> subscriber) {
        return reader;
    }

    @Override
    protected void next(
            rx.observables.AbstractOnSubscribe.SubscriptionState<String, Reader> state) {

        Reader reader = state.state();
        char[] buffer = new char[size];
        try {
            int count = reader.read(buffer);
            if (count == -1)
                state.onCompleted();
            else
                state.onNext(String.valueOf(buffer, 0, count));
        } catch (IOException e) {
            state.onError(e);
        }
    }
}
