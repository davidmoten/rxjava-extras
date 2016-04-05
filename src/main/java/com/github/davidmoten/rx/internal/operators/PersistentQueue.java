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
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.util.Preconditions;

class PersistentQueue<T> implements CloseableQueue<T> {

    int startPosition = 0;
    volatile int readPosition = 0;
    final byte[] startBuffer;
    int startBufferLength = 0;
    final byte[] finishBuffer;
    final RandomAccessFile f;
    final DataSerializer<T> serializer;
    final DataOutput output;
    final DataInput input;
    final File file;
    final AtomicLong size;
    final AtomicReference<WriteInfo> writeInfo = new AtomicReference<WriteInfo>();
    WriteInfo lastWriteInfo = null;

    private static final class WriteInfo {
        final int writePosition;
        final int finishPosition;

        WriteInfo(int writePosition, int finishPosition) {
            this.writePosition = writePosition;
            this.finishPosition = finishPosition;
        }
    }

    public PersistentQueue(int bufferSizeBytes, File file, DataSerializer<T> serializer) {
        Preconditions.checkArgument(bufferSizeBytes > 0,
                "bufferSizeBytes must be greater than zero");
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(!file.exists(), "file exists already");
        Preconditions.checkNotNull(serializer);
        this.startBuffer = new byte[bufferSizeBytes];
        this.finishBuffer = new byte[bufferSizeBytes];
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
        this.writeInfo.set(new WriteInfo(0, 0));
    }

    private static class QueueWriter extends OutputStream {

        private final PersistentQueue<?> q;

        QueueWriter(PersistentQueue<?> queue) {
            this.q = queue;
        }

        @Override
        public void write(int b) throws IOException {
            WriteInfo w = q.writeInfo.get();
            if (w.finishPosition < q.finishBuffer.length) {
                q.finishBuffer[w.finishPosition] = (byte) b;
                q.writeInfo.set(new WriteInfo(w.writePosition, w.finishPosition + 1));
            } else {
                synchronized (this) {
                    q.f.seek(w.writePosition);
                    q.f.write(q.finishBuffer);
                }
                q.writeInfo.set(new WriteInfo(w.writePosition + q.finishBuffer.length, 0));
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
                WriteInfo w = q.writeInfo.get();
                if (q.startPosition < q.startBufferLength) {
                    int b = q.startBuffer[q.startPosition];
                    q.startPosition++;
                    return b;
                } else {
                    if (q.readPosition + q.startBufferLength > w.writePosition) {
                        
                    }
                    q.readPosition += q.startBufferLength;
                    // read a new startBuffer
                    q.startBufferLength = Math.min(w.writePosition - q.readPosition,
                            q.startBuffer.length);
                    synchronized (this) {
                        q.f.seek(q.readPosition);
                        int bytesFromFile = w.writePosition - q.startBufferLength;
                        q.f.read(q.startBuffer, 0, bytesFromFile);
                    }
                    q.startPosition = 1;
                    return q.startBuffer[0];
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
