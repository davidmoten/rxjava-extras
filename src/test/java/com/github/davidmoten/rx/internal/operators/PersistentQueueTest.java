package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Test;

import com.github.davidmoten.rx.buffertofile.DataSerializers;

public class PersistentQueueTest {

    @Test
    public void test() {
        PersistentQueue<Integer> q = createQueue();
        q.offer(1);
        assertEquals(1, (int) q.poll());
        assertNull(q.poll());
    }
    
    @Test
    public void test2() {
        PersistentQueue<Integer> q = createQueue();
        q.offer(1);
        q.offer(2);
        assertEquals(1, (int) q.poll());
        assertEquals(2, (int) q.poll());
        assertNull(q.poll());
    }

    private static PersistentQueue<Integer> createQueue() {
        File file = new File("target/pq");
        file.delete();
        PersistentQueue<Integer> q = new PersistentQueue<Integer>(5, file,
                DataSerializers.integer());
        return q;
    }

}
