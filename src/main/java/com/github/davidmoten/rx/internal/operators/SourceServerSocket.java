package com.github.davidmoten.rx.internal.operators;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.davidmoten.rx.ConnectionNotification;

import rx.AsyncEmitter;
import rx.AsyncEmitter.BackpressureMode;
import rx.AsyncEmitter.Cancellable;
import rx.Notification;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

public class SourceServerSocket {

    public static Observable<ConnectionNotification> create(final int port, final long timeout, final TimeUnit unit) {
        Func0<AsynchronousServerSocketChannel> serverSocketCreator = new Func0<AsynchronousServerSocketChannel>() {

            @Override
            public AsynchronousServerSocketChannel call() {
                try {
                    return AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(port));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Func1<AsynchronousServerSocketChannel, Observable<ConnectionNotification>> serverSocketObservable = new Func1<AsynchronousServerSocketChannel, Observable<ConnectionNotification>>() {

            @Override
            public Observable<ConnectionNotification> call(final AsynchronousServerSocketChannel channel) {
                Action1<AsyncEmitter<ConnectionNotification>> emitterAction = new Action1<AsyncEmitter<ConnectionNotification>>() {

                    @Override
                    public void call(AsyncEmitter<ConnectionNotification> emitter) {
                        channel.accept(null, new Handler(channel, emitter, unit.toMillis(timeout)));

                    }
                };
                return Observable.fromAsync(emitterAction, BackpressureMode.BUFFER);
            }
        };
        Action1<AsynchronousServerSocketChannel> serverSocketDisposer = new Action1<AsynchronousServerSocketChannel>() {

            @Override
            public void call(AsynchronousServerSocketChannel channel) {
                try {
                    channel.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return Observable.using(serverSocketCreator, serverSocketObservable, serverSocketDisposer);
    }

    private static final class Handler
            implements CompletionHandler<AsynchronousSocketChannel, Void> {

        private final AsynchronousServerSocketChannel serverSocketChannel;
        private final AsyncEmitter<ConnectionNotification> emitter;
        private final long timeoutMs;
        
        private volatile boolean keepGoing = true;

        public Handler(AsynchronousServerSocketChannel serverSocketChannel,
                AsyncEmitter<ConnectionNotification> emitter, long timeoutMs) {
            this.serverSocketChannel = serverSocketChannel;
            this.emitter = emitter;
            this.timeoutMs = timeoutMs;
            emitter.setCancellation(new Cancellable() {

                @Override
                public void cancel() throws Exception {
                    keepGoing = false;
                }
            });
        }

        @Override
        public void completed(AsynchronousSocketChannel socketChannel, Void attachment) {
            // listen for new connection
            serverSocketChannel.accept(null, this);
            // Allocate a byte buffer to read from the client
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            String id = UUID.randomUUID().toString();
            try {
                int bytesRead;
                while (keepGoing && (bytesRead = socketChannel.read(buffer).get(timeoutMs,
                        TimeUnit.MILLISECONDS)) != -1) {

                    // Make sure that we have data to read
                    if (buffer.position() > 2) {

                        // Make the buffer ready to read
                        buffer.flip();

                        // copy the current buffer to a byte array
                        byte[] chunk = new byte[bytesRead];
                        buffer.get(chunk, 0, bytesRead);

                        // emit the chunk
                        emitter.onNext(new ConnectionNotification(id, Notification.createOnNext(chunk)));

                        // Make the buffer ready to write
                        buffer.clear();
                    }
                }
                emitter.onNext(new ConnectionNotification(id, Notification.<byte[]>createOnCompleted()));
            } catch (InterruptedException e) {
                error(id, e);
            } catch (ExecutionException e) {
                error(id, e);
            } catch (TimeoutException e) {
                error(id, e);
            }

        }

        @Override
        public void failed(Throwable e, Void attachment) {
            String id = UUID.randomUUID().toString();
            error(id, e);
        }
        
        private void error(String id, Throwable e) {
            emitter.onNext(new ConnectionNotification(id, Notification.<byte[]>createOnError(e)));
        }

    }

}
