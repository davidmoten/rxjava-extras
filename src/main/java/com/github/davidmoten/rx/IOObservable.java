package com.github.davidmoten.rx;

import java.io.InputStream;

import com.github.davidmoten.rx.operators.InputStreamOnSubscribe;

import rx.Observable;

public class IOObservable {

    public static Observable<byte[]> from(InputStream is, int size) {
        return Observable.create(new InputStreamOnSubscribe(is, size));
    }

}
