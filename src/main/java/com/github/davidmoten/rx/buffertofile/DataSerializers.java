package com.github.davidmoten.rx.buffertofile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class DataSerializers {

    private DataSerializers() {
        // prevent instantiation
    }

    public static DataSerializer<String> string() {
        return StringHolder.INSTANCE;
    }

    private static final class StringHolder {
        final static DataSerializer<String> INSTANCE = new DataSerializer<String>() {

            @Override
            public void serialize(DataOutput output, String t) throws IOException {
                output.writeUTF(t);
            }

            @Override
            public String deserialize(DataInput input) throws IOException {
                return input.readUTF();
            }

            @Override
            public int size() {
                return 0;
            }
        };
    }

    public static DataSerializer<Integer> integer() {
        return IntegerHolder.INSTANCE;
    }

    private static final class IntegerHolder {
        final static DataSerializer<Integer> INSTANCE = new DataSerializer<Integer>() {

            @Override
            public void serialize(DataOutput output, Integer t) throws IOException {
                output.writeInt(t);
            }

            @Override
            public Integer deserialize(DataInput input) throws IOException {
                return input.readInt();
            }

            @Override
            public int size() {
                return 4;
            }
        };
    }

    public static DataSerializer<byte[]> byteArray() {
        return ByteArrayHolder.INSTANCE;
    }

    private static final class ByteArrayHolder {
        final static DataSerializer<byte[]> INSTANCE = new DataSerializer<byte[]>() {

            @Override
            public void serialize(DataOutput output, byte[] bytes) throws IOException {
                output.writeInt(bytes.length);
                output.write(bytes);
            }

            @Override
            public byte[] deserialize(DataInput input) throws IOException {
                int length = input.readInt();
                byte[] bytes = new byte[length];
                input.readFully(bytes);
                return bytes;
            }

            @Override
            public int size() {
                return 0;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> DataSerializer<T> javaIO() {
        return (DataSerializer<T>) JavaIOHolder.INSTANCE;
    }

    private static final class JavaIOHolder {

        final static DataSerializer<Object> INSTANCE = new DataSerializer<Object>() {

            @Override
            public void serialize(DataOutput output, Object object) throws IOException {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bytes);
                oos.writeObject(object);
                oos.close();
                byte[] array = bytes.toByteArray();
                output.writeInt(array.length);
                output.write(array);
            }

            @Override
            public Object deserialize(DataInput input) throws IOException {
                int length = input.readInt();
                byte[] array = new byte[length];
                input.readFully(array);
                ObjectInputStream ois = null;
                try {
                    ois = new ObjectInputStream(new ByteArrayInputStream(array));
                    return ois.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (ois != null) {
                        try {
                            ois.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }

            @Override
            public int size() {
                return 0;
            }
        };
    }

}
