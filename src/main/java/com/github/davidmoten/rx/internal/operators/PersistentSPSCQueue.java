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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.util.Preconditions;

class PersistentSPSCQueue<T> implements CloseableQueue<T> {

    public static boolean debug = false;

    int readBufferPosition = 0;
    int readPosition = 0;
    final byte[] readBuffer;
    int readBufferLength = 0;
    final byte[] writeBuffer;
    final File file;
    RandomAccessFile f;
    final DataSerializer<T> serializer;
    final DataOutput output;
    final DataInput input;
    final AtomicLong size;
    volatile int writePosition;
    volatile int writeBufferPosition;
    private final Object fileLock = new Object();
    private final Object writePositionLock = new Object();

    private final PersistentSPSCQueue<T>.QueueWriter queueWriter;
    private final PersistentSPSCQueue<T>.QueueReader queueReader;

    public PersistentSPSCQueue(int bufferSizeBytes, File file, DataSerializer<T> serializer) {
        Preconditions.checkArgument(bufferSizeBytes > 0,
                "bufferSizeBytes must be greater than zero");
        Preconditions.checkNotNull(file);
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
        this.queueWriter = new QueueWriter();
        this.queueReader = new QueueReader();
        this.output = new DataOutputStream(queueWriter);
        this.input = new DataInputStream(queueReader);
        this.size = new AtomicLong(0);
    }

    private final class QueueWriter extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            if (writeBufferPosition < writeBuffer.length) {
                if (debug)
                    log("writeBuffer[" + writeBufferPosition + "]=" + b);
                writeBuffer[writeBufferPosition] = (byte) b;
                writeBufferPosition++;
            } else {
                synchronized (writePositionLock) {
                    synchronized (fileLock) {
                        f.seek(writePosition);
                        f.write(writeBuffer);
                        if (debug)
                            log("wrote buffer " + Arrays.toString(writeBuffer));
                    }

                    writeBuffer[0] = (byte) b;
                    writeBufferPosition = 1;
                    writePosition += writeBuffer.length;
                }
            }
        }

    }

    private static void log(String string) {
        System.out.println(string);
    }

    private final class QueueReader extends InputStream {

        @Override
        public int read() throws IOException {
            if (size.get() == 0) {
                throw new EOFException();
            } else {
                if (readBufferPosition < readBufferLength) {
                    byte b = readBuffer[readBufferPosition];
                    if (debug)
                        log("returned from readBuffer[" + readBufferPosition + "]=" + b);
                    readBufferPosition++;
                    return toUnsignedInteger(b);
                } else {
                    // before reading more we see if we can emit directly from
                    // the writeBuffer by checking if the read position is past
                    // the write position
                    while (true) {
                        int wp;
                        int wbp;
                        synchronized (writePositionLock) {
                            wp = writePosition;
                            wbp = writeBufferPosition;
                        }
                        int over = wp - readPosition;
                        if (over > 0) {
                            synchronized (fileLock) {
                                f.seek(readPosition);
                                readBufferLength = Math.min(readBuffer.length, over);
                                f.read(readBuffer, 0, readBufferLength);
                            }
                            if (debug)
                                log("read buffer " + Arrays.toString(readBuffer));
                            readPosition += readBufferLength;
                            readBufferPosition = 1;
                            if (debug)
                                log("returned from readBuffer[0]=" + readBuffer[0]);
                            return toUnsignedInteger(readBuffer[0]);
                        } else {
                            int index = -over;
                            if (index >= writeBuffer.length) {
                                throw new EOFException();
                            } else {
                                int b;
                                b = toUnsignedInteger(writeBuffer[-over]);
                                if (wp == writePosition && wbp == writeBufferPosition) {
                                    readPosition++;
                                    if (debug)
                                        log("returned from writeBuffer[" + index + "]=" + b);
                                    return b;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static int toUnsignedInteger(byte b) {
        return (int) b & 0x000000FF;
    }

    @Override
    public void unsubscribe() {
        try {
            queueReader.close();
            queueWriter.close();
            f.close();
            if (!file.delete()) {
                throw new RuntimeException("could not delete file " + file);
            }
            if (debug)
                log(Thread.currentThread().getName() + "|persistent queue closed " + file
                        + " exists=" + file.exists());
            f = null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isUnsubscribed() {
        throw new UnsupportedOperationException();
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
            T t = serializer.deserialize(input, Integer.MAX_VALUE);
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
