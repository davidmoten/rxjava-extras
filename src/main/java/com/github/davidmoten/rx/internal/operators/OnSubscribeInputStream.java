package com.github.davidmoten.rx.internal.operators;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import rx.Observer;
import rx.observables.SyncOnSubscribe;

public final class OnSubscribeInputStream extends SyncOnSubscribe<InputStream,byte[]> {

    private final InputStream is;
    private final int size;

    public OnSubscribeInputStream(InputStream is, int size) {
        this.is = is;
        this.size = size;
    }

    @Override
    protected InputStream generateState() {
        return is;
    }

    @Override
    protected InputStream next(InputStream is, Observer<? super byte[]> observer) {
        byte[] buffer = new byte[size];
        try {
            int count = is.read(buffer);
            if (count == -1) {
                observer.onCompleted();
            }
            else if (count < size) {
                observer.onNext(Arrays.copyOf(buffer, count));
            }
            else {
                observer.onNext(buffer);
            }
        } catch (IOException e) {
            observer.onError(e);
        }
        return is;
    }
}
