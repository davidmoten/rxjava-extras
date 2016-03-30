package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.davidmoten.rx.internal.operators.OperatorBufferToFile.DataSerializer;
import com.github.davidmoten.rx.slf4j.Logging;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class OperatorBufferToFileTest {

    private static void reset() {
        File[] files = new File("target").listFiles();
        for (File file : files) {
            if (file.getName().startsWith("db"))
                file.delete();
        }
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

        List<String> b = Observable.just("abc", "def", "ghi")
                .lift(new OperatorBufferToFile<String>(new File("target/db"), serializer, Schedulers.computation())).toList()
                .toBlocking().single();
        assertEquals(Arrays.asList("abc", "def", "ghi"), b);
    }

    @Test
    public void testMany() {
        reset();
        DataSerializer<Integer> serializer = new DataSerializer<Integer>() {

            @Override
            public void serialize(Integer n, DataOutput output) throws IOException {
                output.writeInt(n);
                output.write(new byte[1000000]);
                System.out.println("written " + n);
            }

            @Override
            public Integer deserialize(DataInput input, int size) throws IOException {
                int value = input.readInt();
                input.readFully(new byte[1000000]);
                System.out.println("read " + value);
                return value;
            }
        };
        int max = 1000;
        long t = System.currentTimeMillis();
        int last = Observable.range(1, max)
                .lift(new OperatorBufferToFile<Integer>(new File("target/db"), serializer, Schedulers.computation()))
                .lift(Logging.<Integer> logger().showMemory().log())
                .doOnNext(new Action1<Object>() {

                    @Override
                    public void call(Object t) {
                        try {
                            System.out.println(t);
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            //
                        }

                    }
                }).last().subscribeOn(Schedulers.computation()).toBlocking().single();
        t = System.currentTimeMillis() - t;
        System.out.println("rate = " + (double) max / t * 1000);
        assertEquals(max, last);
    }

}
