package com.github.davidmoten.rx;

import static com.github.davidmoten.rx.Strings.decode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func2;

public class StringsTest {

    @Test
    public void testMultibyteSpanningTwoBuffers() {
        Observable<byte[]> src = Observable.just(new byte[] { (byte) 0xc2 },
                new byte[] { (byte) 0xa1 });
        String out = decode(src, "UTF-8").toBlocking().single();

        assertEquals("\u00A1", out);
    }

    @Test
    public void testMalformedAtTheEndReplace() {
        Observable<byte[]> src = Observable.just(new byte[] { (byte) 0xc2 });
        String out = decode(src, "UTF-8").toBlocking().single();

        // REPLACEMENT CHARACTER
        assertEquals("\uFFFD", out);
    }

    @Test
    public void testMalformedInTheMiddleReplace() {
        Observable<byte[]> src = Observable.just(new byte[] { (byte) 0xc2, 65 });
        String out = decode(src, "UTF-8").toBlocking().single();

        // REPLACEMENT CHARACTER
        assertEquals("\uFFFDA", out);
    }

    @Test(expected = RuntimeException.class)
    public void testMalformedAtTheEndReport() {
        Observable<byte[]> src = Observable.just(new byte[] { (byte) 0xc2 });
        CharsetDecoder charsetDecoder = Charset.forName("UTF-8").newDecoder();
        decode(src, charsetDecoder).toBlocking().single();
    }

    @Test(expected = RuntimeException.class)
    public void testMalformedInTheMiddleReport() {
        Observable<byte[]> src = Observable.just(new byte[] { (byte) 0xc2, 65 });
        CharsetDecoder charsetDecoder = Charset.forName("UTF-8").newDecoder();
        decode(src, charsetDecoder).toBlocking().single();
    }

    @Test
    public void testPropagateError() {
        Observable<byte[]> src = Observable.just(new byte[] { 65 });
        Observable<byte[]> err = Observable.error(new IOException());
        CharsetDecoder charsetDecoder = Charset.forName("UTF-8").newDecoder();
        try {
            decode(Observable.concat(src, err), charsetDecoder).toList().toBlocking().single();
            fail();
        } catch (RuntimeException e) {
            assertEquals(IOException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testPropagateErrorInTheMiddleOfMultibyte() {
        Observable<byte[]> src = Observable.just(new byte[] { (byte) 0xc2 });
        Observable<byte[]> err = Observable.error(new IOException());
        CharsetDecoder charsetDecoder = Charset.forName("UTF-8").newDecoder();
        try {
            decode(Observable.concat(src, err), charsetDecoder).toList().toBlocking().single();
            fail();
        } catch (RuntimeException e) {
            assertEquals(IOException.class, e.getCause().getClass());
        }
    }
    
    @Test
    public void testFromClasspath() {
    	String expected = "hello world\nincoming message";
    	assertEquals(expected, Strings.fromClasspath("/test2.txt").reduce(new Func2<String, String, String>() {
			@Override
			public String call(String a, String b) {
				return a+b;
			}
		}).toBlocking().single());
    }
}
