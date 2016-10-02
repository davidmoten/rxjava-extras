package com.github.davidmoten.rx;

import java.io.IOException;
import java.net.ServerSocket;

import com.github.davidmoten.rx.internal.operators.ObservableServerSocket;
import com.github.davidmoten.rx.util.IORuntimeException;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;

public final class IO {

	private IO() {
		// prevent instantiation
	}

	public static ServerSocketBuilder serverSocket(final int port) {
		return new ServerSocketBuilder(new Func0<ServerSocket>() {

			@Override
			public ServerSocket call() {
				try {
					return new ServerSocket(port);
				} catch (IOException e) {
					throw new IORuntimeException(e);
				}
			}
		});
	}
	
	public static ServerSocketBuilder serverSocketFindAvailablePort(final Action1<Integer> portAction) {
		return serverSocket(new Func0<ServerSocket>() {

			@Override
			public ServerSocket call() {
				try {
					ServerSocket ss = new ServerSocket(0);
					portAction.call(ss.getLocalPort());
					return ss;
				} catch (IOException e) {
					throw new IORuntimeException(e);
				}
			}});
	}

	public static ServerSocketBuilder serverSocket(Func0<? extends ServerSocket> serverSocketFactory) {
		return new ServerSocketBuilder(serverSocketFactory);
	}

	public static class ServerSocketBuilder {

		private final Func0<? extends ServerSocket> serverSocketFactory;
		private int readTimeoutMs = Integer.MAX_VALUE;
		private int bufferSize = 8192;
		private Action0 preAcceptAction = Actions.doNothing0();
		private int acceptTimeoutMs = Integer.MAX_VALUE;

		public ServerSocketBuilder(final Func0<? extends ServerSocket> serverSocketFactory) {
			this.serverSocketFactory = serverSocketFactory;
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
			return ObservableServerSocket.create(serverSocketFactory, readTimeoutMs, bufferSize, preAcceptAction,
					acceptTimeoutMs);
		}

	}

}