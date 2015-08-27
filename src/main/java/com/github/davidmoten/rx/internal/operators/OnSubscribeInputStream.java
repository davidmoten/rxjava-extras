package com.github.davidmoten.rx.internal.operators;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import rx.Subscriber;
import rx.observables.AbstractOnSubscribe;

public final class OnSubscribeInputStream extends AbstractOnSubscribe<byte[], InputStream> {

    private final InputStream is;
    private final int size;

    public OnSubscribeInputStream(InputStream is, int size) {
        this.is = is;
        this.size = size;
    }

    @Override
    protected InputStream onSubscribe(Subscriber<? super byte[]> subscriber) {
        return is;
    }

    @Override
    protected void next(
            rx.observables.AbstractOnSubscribe.SubscriptionState<byte[], InputStream> state) {

        InputStream is = state.state();
        byte[] buffer = new byte[size];
        try {
            int count = is.read(buffer);
            if (count == -1)
                state.onCompleted();
            else if (count < size)
                state.onNext(Arrays.copyOf(buffer, count));
            else
                state.onNext(buffer);
        } catch (IOException e) {
            state.onError(e);
        }
    }
}
