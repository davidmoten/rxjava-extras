package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.buffertofile.CacheType;
import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.rx.buffertofile.Options;
import com.github.davidmoten.rx.slf4j.Logging;

import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public final class OperatorBufferToFileTest {

    @Test
    public void handlesThreeElementsImmediateScheduler() throws InterruptedException {
        checkHandlesThreeElements(createOptions());
    }

    @Test
    public void handlesThreeElementsImmediateSchedulerWeakRef() throws InterruptedException {
        checkHandlesThreeElements(Options.cacheType(CacheType.WEAK_REF).build());
    }

    @Test
    public void handlesThreeElementsImmediateSchedulerSoftRef() throws InterruptedException {
        checkHandlesThreeElements(Options.cacheType(CacheType.SOFT_REF).build());
    }

    @Test
    public void handlesThreeElementsImmediateSchedulerHardRef() throws InterruptedException {
        checkHandlesThreeElements(Options.cacheType(CacheType.HARD_REF).build());
    }

    @Test
    public void handlesThreeElementsImmediateSchedulerLRU() throws InterruptedException {
        checkHandlesThreeElements(Options.cacheType(CacheType.LEAST_RECENTLY_USED).build());
    }

    @Test
    public void handlesThreeElementsImmediateSchedulerWeakWithLimitedCacheAndLimitedStorageSize()
            throws InterruptedException {
        checkHandlesThreeElements(Options.cacheType(CacheType.SOFT_REF).cacheSizeItems(1)
                .storageSizeLimitBytes(10000).build());
    }

    private void checkHandlesThreeElements(Options options) {
        List<String> b = Observable.just("abc", "def", "ghi")
                //
                .compose(Transformers.onBackpressureBufferToFile(createStringSerializer(),
                        Schedulers.immediate(), options))
                .toList().toBlocking().single();
        assertEquals(Arrays.asList("abc", "def", "ghi"), b);
    }

    @Test
    public void handlesThreeElementsImmediateSchedulerSoft() throws InterruptedException {
        List<String> b = Observable.just("abc", "def", "ghi")
                //
                .compose(Transformers.onBackpressureBufferToFile(createStringSerializer(),
                        Schedulers.immediate(), Options.cacheType(CacheType.WEAK_REF).build()))
                .toList().toBlocking().single();
        assertEquals(Arrays.asList("abc", "def", "ghi"), b);
    }

    private static Options createOptions() {
        return Options.cacheType(CacheType.NO_CACHE).build();
    }

    @Test
    public void handlesThreeElementsWithBackpressureAndEnsureCompletionEventArrivesWhenThreeRequested()
            throws InterruptedException {
        TestSubscriber<String> ts = TestSubscriber.create(0);
        Observable.just("abc", "def", "ghi")
                //
                .compose(Transformers.onBackpressureBufferToFile(createStringSerializer(),
                        Schedulers.computation(), createOptions()))
                .subscribe(ts);
        ts.assertNoValues();
        ts.requestMore(2);
        ts.requestMore(1);
        ts.awaitTerminalEvent(10, TimeUnit.SECONDS);
        ts.assertCompleted();
        ts.assertValues("abc", "def", "ghi");
    }

    @Test
    public void handlesErrorSerialization() throws InterruptedException {
        TestSubscriber<String> ts = TestSubscriber.create();
        Observable.<String> error(new IOException("boo"))
                //
                .compose(Transformers.onBackpressureBufferToFile(createStringSerializer(),
                        Schedulers.computation(), createOptions()))
                .subscribe(ts);
        ts.awaitTerminalEvent(10, TimeUnit.SECONDS);
        ts.assertError(IOException.class);
    }

    @Test
    public void handlesErrorWhenDelayErrorIsFalse() throws InterruptedException {
        TestSubscriber<String> ts = TestSubscriber.create(0);
        Observable.just("abc", "def").concatWith(Observable.<String> error(new IOException("boo")))
                //
                .compose(Transformers.onBackpressureBufferToFile(createStringSerializer(),
                        Schedulers.computation(),
                        Options.cacheType(CacheType.NO_CACHE).delayError(false).build()))
                .doOnNext(new Action1<String>() {
                    boolean first = true;

                    @Override
                    public void call(String t) {
                        if (first) {
                            first = false;
                            try {
                                TimeUnit.MILLISECONDS.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                }).subscribe(ts);
        ts.requestMore(2);
        ts.awaitTerminalEvent(5000, TimeUnit.SECONDS);
        ts.assertError(IOException.class);
        ts.assertValueCount(2);
    }

    @Test
    public void handlesUnsubscription() throws InterruptedException {
        TestSubscriber<String> ts = TestSubscriber.create(0);
        Observable.just("abc", "def", "ghi")
                //
                .compose(Transformers.onBackpressureBufferToFile(createStringSerializer(),
                        Schedulers.computation(), createOptions()))
                .subscribe(ts);
        ts.requestMore(2);
        TimeUnit.MILLISECONDS.sleep(500);
        ts.unsubscribe();
        TimeUnit.MILLISECONDS.sleep(500);
        ts.assertValues("abc", "def");
    }

    @Test
    public void handlesUnsubscriptionDuringDrainLoop() throws InterruptedException {
        TestSubscriber<String> ts = TestSubscriber.create(0);
        Observable.just("abc", "def", "ghi")
                //
                .compose(Transformers.onBackpressureBufferToFile(createStringSerializer(),
                        Schedulers.computation(), createOptions()))
                .doOnNext(new Action1<Object>() {

                    @Override
                    public void call(Object t) {
                        try {
                            // pauses drain loop
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                        }
                    }
                }).subscribe(ts);
        ts.requestMore(2);
        TimeUnit.MILLISECONDS.sleep(250);
        ts.unsubscribe();
        TimeUnit.MILLISECONDS.sleep(500);
        ts.assertValues("abc");
    }

    @Test
    public void handlesManyLargeMessages() {
        DataSerializer<Integer> serializer = createLargeMessageSerializer();
        int max = 100;
        int last = Observable.range(1, max)
                //
                .compose(Transformers.onBackpressureBufferToFile(serializer,
                        Schedulers.computation(), createOptions()))
                // log
                .lift(Logging.<Integer> logger().showMemory().log())
                // delay emissions
                .doOnNext(new Action1<Object>() {
                    int count = 0;

                    @Override
                    public void call(Object t) {
                        // delay processing of reads for first three items
                        count++;
                        if (count < 3) {
                            try {
                                System.out.println(t);
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                //
                            }
                        }
                    }
                }).last().toBlocking().single();
        assertEquals(max, last);
    }

    @Test
    public void checkRateForSmallMessages() {
        DataSerializer<Integer> serializer = createIntegerSerializer();
        int max = 10000;
        long t = System.currentTimeMillis();
        int last = Observable.range(1, max)
                //
                .compose(Transformers.onBackpressureBufferToFile(serializer,
                        Schedulers.computation(), Options.cacheType(CacheType.NO_CACHE).build()))
                // log
                .lift(Logging.<Integer> logger().every(1000).showMemory().log()).last().toBlocking()
                .single();
        t = System.currentTimeMillis() - t;
        assertEquals(max, last);
        System.out.println("rate = " + (double) max / (t) * 1000);
        // about 19000 messages per second on i7
    }

    private static DataSerializer<Integer> createIntegerSerializer() {
        DataSerializer<Integer> serializer = new DataSerializer<Integer>() {

            @Override
            public void serialize(DataOutput output, Integer n) throws IOException {
                output.writeInt(n);
            }

            @Override
            public Integer deserialize(DataInput input, int size) throws IOException {
                return input.readInt();
            }
        };
        return serializer;
    }

    private DataSerializer<Integer> createLargeMessageSerializer() {
        DataSerializer<Integer> serializer = new DataSerializer<Integer>() {

            final static int dummyArraySize = 1000000;// 1MB
            final static int chunkSize = 1000;

            @Override
            public void serialize(DataOutput output, Integer n) throws IOException {
                output.writeInt(n);
                // write some filler
                int toWrite = dummyArraySize;
                while (toWrite > 0) {
                    if (toWrite >= chunkSize) {
                        output.write(new byte[chunkSize]);
                        toWrite -= chunkSize;
                    } else {
                        output.write(new byte[toWrite]);
                        toWrite = 0;
                    }
                }
                System.out.println("written " + n);
            }

            @Override
            public Integer deserialize(DataInput input, int size) throws IOException {
                int value = input.readInt();
                // read the filler
                int bytesRead = 0;
                while (bytesRead < dummyArraySize) {
                    if (dummyArraySize - bytesRead >= chunkSize) {
                        input.readFully(new byte[chunkSize]);
                        bytesRead += chunkSize;
                    } else {
                        input.readFully(new byte[dummyArraySize - bytesRead]);
                        bytesRead = dummyArraySize;
                    }
                }
                System.out.println("read " + value);
                return value;
            }
        };
        return serializer;
    }

    private static DataSerializer<String> createStringSerializer() {
        return new DataSerializer<String>() {

            @Override
            public void serialize(DataOutput output, String s) throws IOException {
                output.writeUTF(s);
            }

            @Override
            public String deserialize(DataInput input, int size) throws IOException {
                return input.readUTF();
            }
        };
    }

}
