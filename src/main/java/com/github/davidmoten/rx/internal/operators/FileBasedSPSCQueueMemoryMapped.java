package com.github.davidmoten.rx.internal.operators;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

import com.github.davidmoten.rx.buffertofile.DataSerializer;

import rx.Subscription;

public class FileBasedSPSCQueueMemoryMapped<T> implements Queue<T>, Subscription {

    private final MappedByteBuffer write;
    private final MappedByteBuffer read;
    private final DataSerializer<T> serializer;
    private final DataOutput output;
    private final DataInput input;
    private final int size;

    public FileBasedSPSCQueueMemoryMapped(File file, int size, DataSerializer<T> serializer) {
        this.size = size;
        this.serializer = serializer;
        try {
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            write = f.getChannel().map(MapMode.READ_WRITE, 0, size);
            write.putInt(0);
            write.position(0);
            read = f.getChannel().map(MapMode.READ_ONLY, 0, size);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        output = new DataOutputStream(new MappedByteBufferOutputStream(write));
        input = new DataInputStream(new MappedByteBufferInputStream(read, size));
    }

    private static class MappedByteBufferOutputStream extends OutputStream {

        private final MappedByteBuffer write;

        MappedByteBufferOutputStream(MappedByteBuffer write) {
            this.write = write;
        }

        @Override
        public void write(int b) throws IOException {
            try {
                write.put((byte) b);
            } catch (BufferOverflowException e) {
                throw EOF;
            }
        }

    }

    private static class MappedByteBufferInputStream extends InputStream {

        private final MappedByteBuffer read;
        private final int size;

        MappedByteBufferInputStream(MappedByteBuffer read, int size) {
            this.read = read;
            this.size = size;
        }

        @Override
        public int read() throws IOException {
            if (read.position() < size) {
                return read.get();
            } else
                throw EOF;
        }

    }

    // create the exception once to avoid building many Exception objects
    private static final EOFException EOF = new EOFException();

    @Override
    public boolean offer(T t) {
        int position = write.position();
        try {
            output.writeInt(0);
            serializer.serialize(output, t);
            int length = write.position() - position - 4;
            write.position(position);
            output.writeInt(length);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T poll() {
        int length = read.getInt();
        if (length == 0) {
            read.reset();
            return null;
        } else {
            try {
                T t = serializer.deserialize(input);
                if (t == null) {
                    // this is a trick that we can get away with due to type
                    // erasure in java as long as the return value of poll() is
                    // checked using NullSentinel.isNullSentinel(t) (?)
                    return NullSentinel.instance();
                } else {
                    return t;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isUnsubscribed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unsubscribe() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(T e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T peek() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T element() {
        throw new UnsupportedOperationException();
    }

}
