package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.functions.Action1;

public class AbstractOnSubscribeTest {

    @Test
    public void testInputStream() {
        String text = "hello there, how are ya?";
        final Charset charset = Charset.forName("UTF-8");
        final InputStream is = new ByteArrayInputStream(text.getBytes(charset));
        OnSubscribe<byte[]> onSubscribe = new InputStreamOnSubscribe(is, 2);
        final StringBuilder s = new StringBuilder();
        Observable.create(onSubscribe).forEach(new Action1<byte[]>() {
            @Override
            public void call(byte[] b) {
                s.append(new String(b, charset));
            }
        });
        assertEquals(text, s.toString());
    }
}
