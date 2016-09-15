package com.github.davidmoten.rx;

import com.github.davidmoten.rx.internal.operators.ObservableServerSocket;

import rx.Observable;
import rx.functions.Action0;

public final class IO {

    private IO() {
        // prevent instantiation
    }

    public static ServerSocketBuilder serverSocket(int port) {
        return new ServerSocketBuilder(port);
    }

    public static class ServerSocketBuilder {

        private final int port;
        private int readTimeoutMs = Integer.MAX_VALUE;
        private int bufferSize = 8192;
        private Action0 preAcceptAction = Actions.doNothing0();
        private int acceptTimeoutMs = Integer.MAX_VALUE;

        public ServerSocketBuilder(int port) {
            this.port = port;
        }

        public ServerSocketBuilder readTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
            return this;
        }
        
        public ServerSocketBuilder bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }
        
        public ServerSocketBuilder preAcceptAction(Action0 action) {
            this.preAcceptAction = action;
            return this;
        }
        
        public ServerSocketBuilder acceptTimeoutMs(int acceptTimeoutMs) {
            this.acceptTimeoutMs = acceptTimeoutMs;
            return this;
        }
        
        public Observable<Observable<byte[]>> create() {
            return ObservableServerSocket.create(port, readTimeoutMs, bufferSize, preAcceptAction,
                    acceptTimeoutMs);
        }

    }

}