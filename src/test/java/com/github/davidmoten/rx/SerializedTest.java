package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.Observable;

public class SerializedTest {

    @Test
    public void testSerializeAndDeserializeOfNonEmptyStream() {
        File file = new File("target/temp1");
        file.delete();
        Observable<Integer> source = Observable.just(1, 2, 3);
        Serialized.write(source, file, false).subscribe();
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        List<Integer> list = Serialized.<Integer> read(file).toList().toBlocking().single();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testSerializeAndDeserializeOfNonEmptyStreamWithSmallBuffer() {
        File file = new File("target/temp1");
        file.delete();
        Observable<Integer> source = Observable.just(1, 2, 3);
        Serialized.write(source, file, false, 1).subscribe();
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        List<Integer> list = Serialized.<Integer> read(file, 1).toList().toBlocking().single();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testSerializeAndDeserializeOfEmptyStream() {
        File file = new File("target/temp2");
        file.delete();
        Observable<Integer> source = Observable.empty();
        Serialized.write(source, file, false).subscribe();
        assertTrue(file.exists());
        List<Integer> list = Serialized.<Integer> read(file).toList().toBlocking().single();
        assertTrue(list.isEmpty());
    }

}
