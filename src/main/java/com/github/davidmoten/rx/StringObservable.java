package com.github.davidmoten.rx;

import java.io.InputStream;

import rx.Observable;

public class StringObservable {

    public static Observable<byte[]> from(InputStream is, int size) {
        return Observable.create(new InputStreamOnSubscribe(is, size));
    }

}
