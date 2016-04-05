package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import com.github.davidmoten.rx.buffertofile.DataSerializers;

public class PersistentQueueTest {
    
    @Ignore
    @Test
    public void test() {
        File file = new File("target/pq");
        file.delete();
        PersistentQueue<Integer> q = new PersistentQueue<Integer>(5, file, DataSerializers.integer());
        q.offer(1);
        assertEquals(1, (int) q.poll());
    }

}
