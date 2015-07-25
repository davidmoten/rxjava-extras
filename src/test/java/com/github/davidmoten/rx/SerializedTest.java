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
        Serialized.write(source, file).subscribe();
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        List<Integer> list = Serialized.<Integer> read(file).toList().toBlocking().single();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testSerializeAndDeserializeOfNonEmptyStreamWithSmallBuffer() {
        File file = new File("target/temp2");
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
        File file = new File("target/temp3");
        file.delete();
        Observable<Integer> source = Observable.empty();
        Serialized.write(source, file).subscribe();
        assertTrue(file.exists());
        List<Integer> list = Serialized.<Integer> read(file).toList().toBlocking().single();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSerializeAndDeserializeOfNonEmptyStreamUsingKryo() {
        File file = new File("target/temp4");
        file.delete();
        Observable<Integer> source = Observable.just(1, 2, 3);
        Serialized.kryo().write(source, file).subscribe();
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        List<Integer> list = Serialized.kryo().read(Integer.class, file).toList().toBlocking()
                .single();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testSerializeAndDeserializeOfEmptyStreamUsingKryo() {
        File file = new File("target/temp5");
        file.delete();
        Observable<Integer> source = Observable.empty();
        Serialized.kryo().write(source, file).subscribe();
        assertTrue(file.exists());
        List<Integer> list = Serialized.kryo().read(Integer.class, file).toList().toBlocking()
                .single();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSerializeAndDeserializeOfPersonStreamUsingKryo() {
        File file = new File("target/temp6");
        file.delete();
        Observable<Person> source = Observable.just(new Person("fred", 24), new Person("jane", 32));
        Serialized.kryo().write(source, file).subscribe();
        assertTrue(file.exists());
        List<Person> list = Serialized.kryo().read(Person.class, file).toList().toBlocking()
                .single();
        assertEquals(2, list.size());
        assertEquals("fred", list.get(0).name);
        assertEquals(24, list.get(0).age);
        assertEquals("jane", list.get(1).name);
        assertEquals(32, list.get(1).age);
    }

    static class Person {
        // Note Person class doesn't need to implement Serializable to be
        // serialized by kryo

        final String name;
        final int age;

        Person() {
            // requires no-arg constructor to be serialized by kryo
            this("", 0);
        }

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

    }
}
