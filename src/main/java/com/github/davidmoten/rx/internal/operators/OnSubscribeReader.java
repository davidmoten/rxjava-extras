package com.github.davidmoten.rx.internal.operators;

import java.io.IOException;
import java.io.Reader;

import rx.Observer;
import rx.observables.SyncOnSubscribe;

public final class OnSubscribeReader extends SyncOnSubscribe<Reader,String> {

    private final Reader reader;
    private final int size;

    public OnSubscribeReader(Reader reader, int size) {
        this.reader = reader;
        this.size = size;
    }

    @Override
    protected Reader generateState() {
        return reader;
    }

    @Override
    protected Reader next(Reader reader, Observer<? super String> observer) {
        char[] buffer = new char[size];
        try {
            int count = reader.read(buffer);
            if (count == -1)
                observer.onCompleted();
            else
                observer.onNext(String.valueOf(buffer, 0, count));
        } catch (IOException e) {
            observer.onError(e);
        }
        return reader;
    }
}
