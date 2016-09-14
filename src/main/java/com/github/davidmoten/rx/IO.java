package com.github.davidmoten.rx;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.rx.internal.operators.ObservableServerSocket;

import rx.Observable;

public final class IO {

    private IO() {
        // prevent instantiation
    }

    public static Observable<Observable<byte[]>> serverSocketBasic(int port, long timeout,
            TimeUnit unit, int bufferSize) {
        return ObservableServerSocket.create(port, (int) unit.toMillis(timeout), bufferSize);
    }

}