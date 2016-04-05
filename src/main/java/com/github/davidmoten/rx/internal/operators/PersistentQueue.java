package com.github.davidmoten.rx.internal.operators;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Iterator;

import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.util.Preconditions;

class PersistentQueue<T> implements CloseableQueue<T> {

    int startPosition = 0;
    int finishPosition = 0;
    int readPosition = 0;
    int writePosition = 0;
    final byte[] startBuffer;
    int startBufferLength = 0;
    final byte[] finishBuffer;
    final RandomAccessFile file;
    final DataSerializer<T> serializer;
    final DataOutput output;

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
            this.file = new RandomAccessFile(file, "rw");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.serializer = serializer;
        // TODO check for escape
        this.output = new DataOutputStream(new QueueWriter(this));
    }

    private static class QueueWriter extends OutputStream {

        private final PersistentQueue<?> q;

        QueueWriter(PersistentQueue<?> queue) {
            this.q = queue;
        }

        @Override
        public void write(int b) throws IOException {
            if (q.finishPosition < q.finishBuffer.length) {
                q.finishBuffer[q.finishPosition] = (byte) b;
                q.finishPosition++;
            } else {
                q.file.seek(q.writePosition);
                q.file.write(q.finishBuffer);
                q.writePosition += q.finishBuffer.length;
                q.finishPosition = 0;
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
            if (q.startPosition < q.startBufferLength) {
                int b = q.startBuffer[q.startPosition];
                q.startPosition++;
                return b;
            } else {
                q.readPosition += q.startBuffer.length;
                q.file.seek(q.readPosition);
                q.file.read(q.startBuffer);
                return 0;
            }
        }

    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean offer(T e) {
        return true;
    }

    @Override
    public T poll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
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
