package com.github.davidmoten.rx.internal.operators;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.rx.internal.operators.FileBasedSPSCQueueMemoryMappedReaderWriter.EOFRuntimeException;
import com.github.davidmoten.util.Preconditions;

import rx.functions.Func0;

public final class FileBasedSPSCQueueMemoryMapped<T> implements QueueWithSubscription<T> {

    private final Queue<FileBasedSPSCQueueMemoryMappedReaderWriter<T>> inactive = new LinkedList<FileBasedSPSCQueueMemoryMappedReaderWriter<T>>();
    private final Deque<FileBasedSPSCQueueMemoryMappedReaderWriter<T>> toRead = new ArrayDeque<FileBasedSPSCQueueMemoryMappedReaderWriter<T>>();
    private final Object lock = new Object();
    private final Func0<File> factory;
    private final int size;
    // only needs to be visible to thread calling poll()
    private FileBasedSPSCQueueMemoryMappedReaderWriter<T> reader;
    // only needs to be visible to thread calling offer()
    private FileBasedSPSCQueueMemoryMappedReaderWriter<T> writer;
    private final AtomicInteger wip = new AtomicInteger();
    private volatile boolean unsubscribed = false;
    private final AtomicLong count = new AtomicLong();

    private final DataSerializer<T> serializer;

    public FileBasedSPSCQueueMemoryMapped(Func0<File> factory, int size,
            DataSerializer<T> serializer) {
        Preconditions.checkNotNull(factory);
        Preconditions.checkNotNull(serializer);
        this.factory = factory;
        this.size = size;
        this.serializer = serializer;
        File file = factory.call();
        this.writer = new FileBasedSPSCQueueMemoryMappedReaderWriter<T>(file, size, serializer);
        this.reader = writer.openForWrite().openForRead();
        // store store barrier
        wip.lazySet(0);
    }

    @Override
    public void unsubscribe() {
        wip.incrementAndGet();
        unsubscribed = true;
        checkUnsubscribe();
    }

    @Override
    public boolean isUnsubscribed() {
        return unsubscribed;
    }

    @Override
    public synchronized boolean offer(T t) {
        // thread safe with poll() and unsubscribe()
        try {
            wip.incrementAndGet();
            if (unsubscribed)
                return true;
            if (!writer.offer(t)) {
                // note that writer will be in a closed state if we follow this
                // path
                FileBasedSPSCQueueMemoryMappedReaderWriter<T> nextWriter;
                synchronized (lock) {
                    nextWriter = inactive.poll();
                    if (nextWriter == null) {
                        nextWriter = new FileBasedSPSCQueueMemoryMappedReaderWriter<T>(
                                factory.call(), size, serializer);
                    }
                    toRead.offerLast(nextWriter);
                    nextWriter.openForWrite();
                }
                writer = nextWriter;
                return writer.offer(t);
            } else {
                return true;
            }
        } finally {
            checkUnsubscribe();
            count.incrementAndGet();
        }
    }

    private void checkUnsubscribe() {
        // single ampersand because we must call wip.decrementAndGet
        if (unsubscribed & wip.decrementAndGet() == 0) {
            close();
        }
    }

    private void close() {
        writer.close();
        reader.close();
    }

    @Override
    public synchronized T poll() {
        T value = null;
        // thread safe with offer() and unsubscribe()
        try {
            wip.incrementAndGet();
            if (unsubscribed)
                return null;
            value = reader.poll();
            return value;
        } catch (EOFRuntimeException e) {
            FileBasedSPSCQueueMemoryMappedReaderWriter<T> nextReader;
            synchronized (lock) {
                if (toRead.isEmpty()) {
                    return null;
                } else {
                    nextReader = toRead.pollFirst();
                }
                reader.closeForRead();
                inactive.offer(reader);
            }
            reader = nextReader;
            reader.openForRead();
            value = reader.poll();
            return value;
        } finally {
            checkUnsubscribe();
            if (value != null) {
                count.decrementAndGet();
            }
        }
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return count.get() == 0;
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

    @Override
    public boolean add(T e) {
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

    @Override
    public T peek() {
        throw new UnsupportedOperationException();
    }

}
