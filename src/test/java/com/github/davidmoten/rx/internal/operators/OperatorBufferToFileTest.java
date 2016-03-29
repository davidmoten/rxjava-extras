package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mapdb.Serializer;

import rx.Notification;
import rx.Observable;

public class OperatorBufferToFileTest {

    @Test
    public void testSerialization() throws IOException {
        Notification<String> n = Notification.createOnNext("abc");
        MySerializer m = new MySerializer();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutput output = new DataOutputStream(bytes);
        m.serialize(output, n);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes.toByteArray());
        DataInput input = new DataInputStream(is);
        System.out.println(m.deserialize(input, bytes.size()));
    }

    @Test
    public void test() {

        List<String> b = Observable.just("abc", "def", "ghi")
                .lift(new OperatorBufferToFile<String>(new File("target/db"), new MySerializer()))
                .toList().toBlocking().single();
        assertEquals(Arrays.asList("abc", "def", "ghi"), b);
    }

    public static final class MySerializer
            implements Serializer<Notification<String>>, Serializable {

        private static final long serialVersionUID = -4992031045087289671L;

        @Override
        public Notification<String> deserialize(DataInput input, int size) throws IOException {
            byte type = input.readByte();
            if (type == 0) {
                return Notification.createOnCompleted();
            } else if (type == 1) {
                String errorClass = input.readUTF();
                String message = input.readUTF();
                return Notification.createOnError(new RuntimeException(errorClass + ":" + message));
            } else {
                String value = input.readUTF();
                return Notification.createOnNext(value);
            }
        }

        @Override
        public int fixedSize() {
            return -1;
        }

        @Override
        public void serialize(DataOutput output, Notification<String> n) throws IOException {
            if (n.isOnCompleted()) {
                output.writeByte(0);
            } else if (n.isOnError()) {
                output.writeByte(1);
                output.writeUTF(n.getThrowable().getClass().getName());
                output.writeUTF(n.getThrowable().getMessage());
            } else {
                output.writeByte(2);
                output.writeUTF(n.getValue());
            }
        }
    };
    
}
