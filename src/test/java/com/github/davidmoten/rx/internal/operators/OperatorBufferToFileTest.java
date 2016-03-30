package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.rx.slf4j.Logging;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class OperatorBufferToFileTest {

    private static void reset() {
        System.out.println("deleting db");
        File[] files = new File("target").listFiles();
        for (File file : files) {
            if (file.getName().startsWith("db"))
                file.delete();
        }
        System.out.println("deleted db");
    }

    @Test
    public void testThreeElements() {
        reset();
        DataSerializer<String> serializer = new DataSerializer<String>() {

            @Override
            public void serialize(String s, DataOutput output) throws IOException {
                output.writeUTF(s);
            }

            @Override
            public String deserialize(DataInput input, int size) throws IOException {
                return input.readUTF();
            }
        };

        List<String> b = Observable
                .just("abc", "def", "ghi").compose(Transformers
                        .onBackpressureBufferToFile(serializer, Schedulers.computation()))
                .toList().toBlocking().single();
        assertEquals(Arrays.asList("abc", "def", "ghi"), b);
    }

    @Test
    public void testMany() {
        reset();
        DataSerializer<Integer> serializer = new DataSerializer<Integer>() {

            final static int dummyArraySize = 1000000;// 1MB
            final static int chunkSize = 1000;

            @Override
            public void serialize(Integer n, DataOutput output) throws IOException {
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
        int max = 100;
        long t = System.currentTimeMillis();
        int last = Observable.range(1, max)
                .compose(Transformers.onBackpressureBufferToFile(serializer,
                        Schedulers.computation()))
                .lift(Logging.<Integer> logger().showMemory().log())
                .doOnNext(new Action1<Object>() {
                    int count = 0;

                    @Override
                    public void call(Object t) {
                        // delay processing of reads for
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
                }).last().subscribeOn(Schedulers.computation()).toBlocking().single();
        t = System.currentTimeMillis() - t;
        System.out.println("rate = " + (double) max / t * 1000);
        assertEquals(max, last);
    }

}