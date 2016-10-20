package com.github.davidmoten.rx.internal.operators;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import com.github.davidmoten.rx.Actions;
import com.github.davidmoten.rx.Bytes;
import com.github.davidmoten.rx.Checked;
import com.github.davidmoten.rx.Checked.F0;
import com.github.davidmoten.rx.Functions;

import rx.Observable;
import rx.Observer;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.SyncOnSubscribe;

public final class ObservableServerSocket {

    private ObservableServerSocket() {
        // prevent instantiation
    }

    public static Observable<Observable<byte[]>> create(
            final Func0<? extends ServerSocket> serverSocketFactory, final int timeoutMs,
            final int bufferSize, Action0 preAcceptAction, int acceptTimeoutMs,
            Func1<? super Socket, Boolean> acceptSocket) {
        Func1<ServerSocket, Observable<Observable<byte[]>>> observableFactory = createObservableFactory(
                timeoutMs, bufferSize, preAcceptAction, acceptSocket);
        return Observable.<Observable<byte[]>, ServerSocket> using( //
                createServerSocketFactory(serverSocketFactory, acceptTimeoutMs), //
                observableFactory, //
                new Action1<ServerSocket>() {

                    @Override
                    public void call(ServerSocket ss) {
                        try {
                            ss.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }, true);
    }

    private static Func0<ServerSocket> createServerSocketFactory(
            final Func0<? extends ServerSocket> serverSocketFactory, final int acceptTimeoutMs) {
        return Checked.f0(new F0<ServerSocket>() {
            @Override
            public ServerSocket call() throws Exception {
                return createServerSocket(serverSocketFactory, acceptTimeoutMs);
            }
        });
    }

    private static ServerSocket createServerSocket(
            Func0<? extends ServerSocket> serverSocketCreator, long timeoutMs) throws IOException {
        ServerSocket s = serverSocketCreator.call();
        s.setSoTimeout((int) timeoutMs);
        return s;
    }

    private static Func1<ServerSocket, Observable<Observable<byte[]>>> createObservableFactory(
            final int timeoutMs, final int bufferSize, final Action0 preAcceptAction,
            final Func1<? super Socket, Boolean> acceptSocket) {
        return new Func1<ServerSocket, Observable<Observable<byte[]>>>() {
            @Override
            public Observable<Observable<byte[]>> call(ServerSocket serverSocket) {
                return createServerSocketObservable(serverSocket, timeoutMs, bufferSize,
                        preAcceptAction, acceptSocket);
            }
        };
    }

    private static Observable<Observable<byte[]>> createServerSocketObservable(
            ServerSocket serverSocket, final long timeoutMs, final int bufferSize,
            final Action0 preAcceptAction, final Func1<? super Socket, Boolean> acceptSocket) {
        return Observable.create( //
                SyncOnSubscribe.<ServerSocket, Observable<byte[]>> createSingleState( //
                        Functions.constant0(serverSocket), //
                        new Action2<ServerSocket, Observer<? super Observable<byte[]>>>() {

                            @Override
                            public void call(ServerSocket ss,
                                    Observer<? super Observable<byte[]>> observer) {
                                acceptConnection(timeoutMs, bufferSize, ss, observer,
                                        preAcceptAction, acceptSocket);
                            }
                        }));
    }

    private static void acceptConnection(long timeoutMs, int bufferSize, ServerSocket ss,
            Observer<? super Observable<byte[]>> observer, Action0 preAcceptAction,
            Func1<? super Socket, Boolean> acceptSocket) {
        Socket socket;
        while (true) {
            try {
                preAcceptAction.call();
                socket = ss.accept();
                if (!acceptSocket.call(socket)) {
                    closeQuietly(socket);
                } else {
                    observer.onNext(createSocketObservable(socket, timeoutMs, bufferSize));
                    break;
                }
            } catch (SocketTimeoutException e) {
                // timed out so will loop around again
            } catch (IOException e) {
                // unknown problem
                observer.onError(e);
                break;
            }
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // ignore exception
        }
    }

    private static Observable<byte[]> createSocketObservable(final Socket socket, long timeoutMs,
            final int bufferSize) {
        setTimeout(socket, timeoutMs);
        return Observable.using( //
                Checked.f0(new F0<InputStream>() {
                    @Override
                    public InputStream call() throws Exception {
                        return socket.getInputStream();
                    }
                }), //
                new Func1<InputStream, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(InputStream is) {
                        return Bytes.from(is, bufferSize);
                    }
                }, //
                Actions.close(), //
                true);
    }

    private static void setTimeout(Socket socket, long timeoutMs) {
        try {
            socket.setSoTimeout((int) timeoutMs);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

}
