package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import rx.Observable;

public class SerializedTest {

    @Test
    public void testSerializeAndDeserialize() {
        File file = new File("target/temp123");
        Observable<Integer> source = Observable.just(1, 2, 3);
        Serialized.write(source, file, false).subscribe();
        List<Integer> list = Serialized.<Integer> from(file).toList().toBlocking().single();
        assertEquals(Arrays.asList(1,2,3),list);
    }

}
