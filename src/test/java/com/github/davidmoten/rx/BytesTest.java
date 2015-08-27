package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.davidmoten.rx.internal.operators.MyZipEntry;

import rx.Observable;
import rx.functions.Func1;

public class BytesTest {

    @Test
    public void testUnzip() {
        InputStream is = BytesTest.class.getResourceAsStream("/test.zip");
        assertNotNull(is);
        List<String> list = Bytes.unzip(is).concatMap(new Func1<MyZipEntry, Observable<String>>() {

            @Override
            public Observable<String> call(MyZipEntry entry) {
                return Observable.just(entry.getName())
                        .concatWith(Strings.from(entry.getInputStream()));
            }
        }).toList().toBlocking().single();
        assertEquals(Arrays.asList("document1.txt", "hello there", "document2.txt",
                "how are you going?"), list);
    }

}
