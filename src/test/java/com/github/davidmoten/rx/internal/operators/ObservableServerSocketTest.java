package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.github.davidmoten.junit.Asserts;
import com.github.davidmoten.rx.Actions;
import com.github.davidmoten.rx.Bytes;
import com.github.davidmoten.rx.IO;
import com.github.davidmoten.rx.RetryWhen;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

public final class ObservableServerSocketTest {
    
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final int PORT = 12345;
    private static final String TEXT = "hello there";

    private static final int POOL_SIZE = 10;
    private static final Scheduler scheduler = Schedulers
            .from(Executors.newFixedThreadPool(POOL_SIZE));
    
    private static final Scheduler clientScheduler = Schedulers
            .from(Executors.newFixedThreadPool(POOL_SIZE));

    @Test
    public void serverSocketReadsTcpPushWhenBufferIsSmallerThanInput()
            throws UnknownHostException, IOException, InterruptedException {
        checkServerSocketReadsTcpPushWhenBufferSizeIs(TEXT, 4);
    }

    @Test
    public void serverSocketReadsTcpPushWhenBufferIsBiggerThanInput()
            throws UnknownHostException, IOException, InterruptedException {
        checkServerSocketReadsTcpPushWhenBufferSizeIs(TEXT, 8192);
    }

    @Test
    public void serverSocketReadsTcpPushWhenBufferIsSameSizeAsInput()
            throws UnknownHostException, IOException, InterruptedException {
        checkServerSocketReadsTcpPushWhenBufferSizeIs(TEXT, TEXT.length());
    }

    @Test
    public void serverSocketReadsTcpPushWhenInputIsEmpty()
            throws UnknownHostException, IOException, InterruptedException {
        checkServerSocketReadsTcpPushWhenBufferSizeIs("", 4);
    }

    @Test
    public void serverSocketReadsTcpPushWhenInputIsOneCharacter()
            throws UnknownHostException, IOException, InterruptedException {
        checkServerSocketReadsTcpPushWhenBufferSizeIs("a", 4);
    }

    @Test
    public void errorEmittedIfServerSocketBusy() throws IOException {
        reset();
        TestSubscriber<Object> ts = TestSubscriber.create();
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(PORT);
            IO.serverSocket(PORT).readTimeoutMs(10000).bufferSize(5).create().subscribe(ts);
            ts.assertNoValues();
            ts.assertNotCompleted();
            ts.assertTerminalEvent();
            assertTrue(ts.getOnErrorEvents().get(0).getCause() instanceof BindException);
        } finally {
            socket.close();
        }
    }

    @Test
    public void isUtilityClass() {
        Asserts.assertIsUtilityClass(ObservableServerSocket.class);
    }

    @Test
    public void isUtilityClassIO() {
        Asserts.assertIsUtilityClass(IO.class);
    }

    @Test
    public void testCloserWhenDoesNotThrow() {
        final AtomicBoolean called = new AtomicBoolean();
        Closeable c = new Closeable() {

            @Override
            public void close() throws IOException {
                called.set(true);
            }
        };
        Actions.close().call(c);
        assertTrue(called.get());
    }

    @Test
    public void testCloserWhenThrows() {
        final IOException ex = new IOException();
        Closeable c = new Closeable() {

            @Override
            public void close() throws IOException {
                throw ex;
            }
        };
        try {
            Actions.close().call(c);
            Assert.fail();
        } catch (RuntimeException e) {
            assertTrue(ex == e.getCause());
        }
    }

    private static void reset() {
        com.github.davidmoten.rx.Schedulers.blockUntilWorkFinished(scheduler, POOL_SIZE);
        com.github.davidmoten.rx.Schedulers.blockUntilWorkFinished(clientScheduler, POOL_SIZE);
    }

    @Test
    public void testEarlyUnsubscribe()
            throws UnknownHostException, IOException, InterruptedException {
        reset();
        TestSubscriber<Object> ts = TestSubscriber.create();
        final AtomicReference<byte[]> result = new AtomicReference<byte[]>();
        try {
            int bufferSize = 4;
            IO.serverSocket(PORT).readTimeoutMs(10000).bufferSize(bufferSize).create() //
                    .flatMap(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                        @Override
                        public Observable<byte[]> call(Observable<byte[]> g) {
                            return g //
                                    .first() //
                                    .compose(Bytes.collect()) //
                                    .doOnNext(Actions.setAtomic(result)) //
                                    .doOnNext(new Action1<byte[]>() {
                                        @Override
                                        public void call(byte[] bytes) {
                                            System.out.println(Thread.currentThread().getName()
                                                    + ": " + new String(bytes));
                                        }
                                    }) //
                                    .onErrorResumeNext(Observable.<byte[]> empty()) //
                                    .subscribeOn(scheduler);
                        }
                    }) //
                    .subscribeOn(scheduler) //
                    .subscribe(ts);
            Thread.sleep(300);
            Socket socket = new Socket("localhost", PORT);
            OutputStream out = socket.getOutputStream();
            out.write("12345678901234567890".getBytes());
            out.close();
            socket.close();
            Thread.sleep(1000);
            assertEquals("1234", new String(result.get(), UTF_8));
        } finally {
            // will close server socket
            ts.unsubscribe();
        }
    }

    @Test
    public void testCancelDoesNotHaveToWaitForTimeout()
            throws UnknownHostException, IOException, InterruptedException {
        reset();
        RxJavaHooks.setOnError(Actions.printStackTrace1());
        TestSubscriber<Object> ts = TestSubscriber.create();
        final AtomicReference<byte[]> result = new AtomicReference<byte[]>();
        try {
            int bufferSize = 4;
            IO.serverSocket(PORT).readTimeoutMs(Integer.MAX_VALUE).bufferSize(bufferSize).create()
                    .flatMap(new Func1<Observable<byte[]>, Observable<String>>() {
                        @Override
                        public Observable<String> call(Observable<byte[]> g) {
                            return g //
                                    .first() //
                                    .compose(Bytes.collect()) //
                                    .doOnNext(Actions.setAtomic(result)) //
                                    .map(new Func1<byte[], String>() {
                                        @Override
                                        public String call(byte[] bytes) {
                                            return new String(bytes, UTF_8);
                                        }
                                    }) //
                                    .doOnNext(new Action1<String>() {
                                        @Override
                                        public void call(String s) {
                                            System.out.println(
                                                    Thread.currentThread().getName() + ": " + s);
                                        }
                                    }) //
                                    .onErrorResumeNext(Observable.<String> empty()) //
                                    .subscribeOn(scheduler);
                        }
                    }).subscribeOn(scheduler) //
                    .subscribe(ts);
            Thread.sleep(300);
            @SuppressWarnings("resource")
            Socket socket = new Socket("localhost", PORT);
            OutputStream out = socket.getOutputStream();
            out.write("hell".getBytes(UTF_8));
            out.flush();
            Thread.sleep(500);
            assertEquals(Arrays.asList("hell"), ts.getOnNextEvents());
            ts.assertNoTerminalEvent();
            out.write("will-fail".getBytes(UTF_8));
            out.flush();
        } finally {
            // will close server socket
            try {
                ts.unsubscribe();
                Thread.sleep(300);
            } finally {
                RxJavaHooks.reset();
            }
        }
    }

    @Test
    public void testLoad() throws InterruptedException {
        reset();
        AtomicBoolean errored = new AtomicBoolean(false);
        for (int k = 0; k < 1; k++) {
            System.out.println("loop " + k);
            TestSubscriber<String> ts = TestSubscriber.create();
            final AtomicInteger connections = new AtomicInteger();
            try {
                int bufferSize = 4;
                IO.serverSocket(PORT).readTimeoutMs(10000).bufferSize(bufferSize).create()
                        .flatMap(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                            @Override
                            public Observable<byte[]> call(Observable<byte[]> g) {
                                return g //
                                        .doOnSubscribe(Actions.increment0(connections)) //
                                        .compose(Bytes.collect()) //
                                        .doOnError(Actions.printStackTrace1()) //
                                        .subscribeOn(scheduler) //
                                        .retryWhen(RetryWhen.delay(1, TimeUnit.SECONDS).build());
                            }
                        }, 1) //
                        .map(new Func1<byte[], String>() {
                            @Override
                            public String call(byte[] bytes) {
                                return new String(bytes, UTF_8);
                            }
                        }) //
                        .doOnNext(Actions.decrement1(connections)) //
                        .doOnError(Actions.printStackTrace1()) //
                        .doOnError(Actions.<Throwable> setToTrue1(errored)) //
                        .subscribeOn(scheduler) //
                        .subscribe(ts);
                TestSubscriber<Object> ts2 = TestSubscriber.create();
                final Set<String> messages = new ConcurrentSkipListSet<String>();

                final int messageBlocks = 10;
                int numMessages = 1000;

                final AtomicInteger openSockets = new AtomicInteger(0);
                // sender
                Observable.range(1, numMessages).flatMap(new Func1<Integer, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(Integer n) {
                        return Observable.defer(new Func0<Observable<Object>>() {
                            @Override
                            public Observable<Object> call() {
                                // System.out.println(Thread.currentThread().getName()
                                // +
                                // " - writing message");
                                String id = UUID.randomUUID().toString();
                                StringBuilder s = new StringBuilder();
                                for (int i = 0; i < messageBlocks; i++) {
                                    s.append(id);
                                }
                                messages.add(s.toString());
                                Socket socket = null;
                                try {
                                    socket = new Socket("localhost", PORT);
                                    // allow reuse so we don't run out of
                                    // sockets
                                    socket.setReuseAddress(true);
                                    socket.setSoTimeout(5000);
                                    int count = openSockets.incrementAndGet();
                                    
                                    OutputStream out = socket.getOutputStream();
                                    for (int i = 0; i < messageBlocks; i++) {
                                        out.write(id.getBytes(UTF_8));
                                    }
                                    out.close();
                                    count = openSockets.decrementAndGet();
                                    // System.out.println("open sockets=" +
                                    // count + ",
                                    // connections = "
                                    // + connections.get());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                } finally {
                                    try {
                                        socket.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                return Observable.<Object> just(1);
                            }
                        }) //
                                .timeout(5, TimeUnit.SECONDS) //
                                .subscribeOn(clientScheduler);
                    }
                }) //
                        .doOnError(Actions.printStackTrace1()) //
                        .subscribe(ts2);
                ts2.awaitTerminalEvent();
                ts2.assertCompleted();
                // allow server to complete processing
                Thread.sleep(1000);
                assertEquals(messages, new HashSet<String>(ts.getOnNextEvents()));
                assertFalse(errored.get());
            } finally {
                ts.unsubscribe();
                reset();
            }
        }
    }

    private void checkServerSocketReadsTcpPushWhenBufferSizeIs(String text, int bufferSize)
            throws UnknownHostException, IOException, InterruptedException {
        reset();
        TestSubscriber<Object> ts = TestSubscriber.create();
        final AtomicReference<byte[]> result = new AtomicReference<byte[]>();
        try {
            IO.serverSocket(PORT).readTimeoutMs(10000).bufferSize(bufferSize).create()
                    .flatMap(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                        @Override
                        public Observable<byte[]> call(Observable<byte[]> g) {
                            return g //
                                    .compose(Bytes.collect()) //
                                    .doOnNext(Actions.setAtomic(result)) //
                                    .doOnNext(new Action1<byte[]>() {
                                        @Override
                                        public void call(byte[] bytes) {
                                            System.out.println(Thread.currentThread().getName()
                                                    + ": " + new String(bytes));
                                        }
                                    }) //
                                    .onErrorResumeNext(Observable.<byte[]> empty()) //
                                    .subscribeOn(scheduler);
                        }
                    }).subscribeOn(scheduler) //
                    .subscribe(ts);
            Thread.sleep(1000);
            Socket socket = new Socket("localhost", PORT);
            OutputStream out = socket.getOutputStream();
            out.write(text.getBytes());
            out.close();
            socket.close();
            Thread.sleep(1000);
            assertEquals(text, new String(result.get(), UTF_8));
        } finally {
            // will close server socket
            ts.unsubscribe();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        reset();
        TestSubscriber<Object> ts = TestSubscriber.create();
        IO.serverSocket(PORT).readTimeoutMs(10000).bufferSize(8).create()
                .flatMap(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(Observable<byte[]> g) {
                        return g //
                                .compose(Bytes.collect()) //
                                .doOnNext(new Action1<byte[]>() {

                                    @Override
                                    public void call(byte[] bytes) {
                                        System.out.println(Thread.currentThread().getName() + ": "
                                                + new String(bytes).trim());
                                    }
                                }) //
                                .onErrorResumeNext(Observable.<byte[]> empty()) //
                                .subscribeOn(scheduler);
                    }
                }).subscribeOn(scheduler) //
                .subscribe(ts);

        Thread.sleep(10000000);

    }
}

