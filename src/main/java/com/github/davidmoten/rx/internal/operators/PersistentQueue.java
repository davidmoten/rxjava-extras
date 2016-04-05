package com.github.davidmoten.rx.internal.operators;

import java.io.BufferedReader;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.util.Preconditions;

class PersistentQueue<T> implements CloseableQueue<T> {

    int readBufferPosition = 0;
    volatile int readPosition = 0;
    final byte[] readBuffer;
    int readBufferLength = 0;
    final byte[] writeBuffer;
    final RandomAccessFile f;
    final DataSerializer<T> serializer;
    final DataOutput output;
    final DataInput input;
    final File file;
    final AtomicLong size;
    volatile int writePosition;
    volatile int writeBufferPosition;

    public PersistentQueue(int bufferSizeBytes, File file, DataSerializer<T> serializer) {
        Preconditions.checkArgument(bufferSizeBytes > 0,
                "bufferSizeBytes must be greater than zero");
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(!file.exists(), "file exists already");
        Preconditions.checkNotNull(serializer);
        this.readBuffer = new byte[bufferSizeBytes];
        this.writeBuffer = new byte[bufferSizeBytes];
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
            this.file = file;
            this.f = new RandomAccessFile(file, "rw");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.serializer = serializer;
        this.output = new DataOutputStream(new QueueWriter(this));
        this.input = new DataInputStream(new QueueReader(this));
        this.size = new AtomicLong(0);
    }

    private static class QueueWriter extends OutputStream {

        private final PersistentQueue<?> q;

        QueueWriter(PersistentQueue<?> queue) {
            this.q = queue;
        }

        @Override
        public void write(int b) throws IOException {
            if (q.writeBufferPosition < q.writeBuffer.length) {
                System.out.println("writeBuffer[" + q.writeBufferPosition + "]=" + b);
                q.writeBuffer[q.writeBufferPosition] = (byte) b;
                q.writeBufferPosition++;
            } else {
                synchronized (this) {
                    q.f.seek(q.writePosition);
                    q.f.write(q.writeBuffer);
                    System.out.println("wrote buffer " + Arrays.toString(q.writeBuffer));
                }
                q.writeBuffer[0] = (byte) b;
                q.writeBufferPosition = 1;
                q.writePosition += q.writeBuffer.length;
            }
        }

    }

    private static class QueueReader extends InputStream {

        private final PersistentQueue<?> q;

        QueueReader(PersistentQueue<?> queue) {
            this.q = queue;
        }

        @Override
        public int read() throws IOException {
            if (q.size.get() == 0) {
                throw new EOFException();
            } else {
                if (q.readBufferPosition < q.readBufferLength) {
                    int b = q.readBuffer[q.readBufferPosition];
                    System.out
                            .println("returned from readBuffer[" + q.readBufferPosition + "]=" + b);
                    q.readBufferPosition++;
                    return b;
                } else {
                    while (true) {
                        int wp = q.writePosition;
                        int wbp = q.writeBufferPosition;
                        int over = wp - q.readPosition;
                        if (over > 0) {
                            synchronized (this) {
                                q.f.seek(q.readPosition);
                                q.readBufferLength = Math.min(q.readBuffer.length, over);
                                q.f.read(q.readBuffer, 0, q.readBufferLength);
                                System.out.println("read buffer " + Arrays.toString(q.readBuffer));
                            }
                            q.readPosition += q.readBufferLength;
                            q.readBufferPosition = 1;
                            System.out.println("returned from readBuffer[0]=" + q.readBuffer[0]);
                            return q.readBuffer[0];
                        } else {
                            int b = q.writeBuffer[-over];
                            if (wp == q.writePosition && wbp == q.writeBufferPosition) {
                                q.readPosition++;
                                System.out
                                        .println("returned from writeBuffer[" + (-over) + "]=" + b);
                                return b;
                            }
                        }
                    }
                }
            }
        }

    }

    @Override
    public void close() {
        try {
            BufferedReader r = null;
            f.getChannel().close();
            if (!file.delete()) {
                throw new RuntimeException("could not delete file " + file);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean offer(T t) {
        try {
            serializer.serialize(output, t);
            size.incrementAndGet();
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T poll() {
        try {
            T t = serializer.deserialize(input, -1);
            size.decrementAndGet();
            return t;
        } catch (EOFException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isEmpty() {
        return size.get() == 0;
    }

    @Override
    public T element() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T peek() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(T e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove() {
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

    @SuppressWarnings("hiding")
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

}
