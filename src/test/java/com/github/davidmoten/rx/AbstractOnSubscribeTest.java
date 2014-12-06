package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Test;

import rx.Observable;
import rx.functions.Action1;

public class AbstractOnSubscribeTest {

    @Test
    public void testInputStream() {
        final InputStream is = new ByteArrayInputStream("hello there!".getBytes(Charset
                .forName("UTF-8")));
        AbstractOnSubscribe<byte[]> onSubscribe = new AbstractOnSubscribe<byte[]>() {

            @Override
            public Optional<byte[]> next() {
                try {
                    byte[] bytes = new byte[2];
                    int n = is.read(bytes);
                    if (n == -1)
                        return Optional.absent();
                    else
                        return Optional.of(Arrays.copyOf(bytes, n));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        final StringBuilder s = new StringBuilder();
        Observable.create(onSubscribe).forEach(new Action1<byte[]>() {

            @Override
            public void call(byte[] b) {
                s.append(new String(b, Charset.forName("UTF-8")));
            }
        });
        assertEquals("hello there!", s.toString());
    }
}
