package com.github.davidmoten.rx.internal.operators;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.rx.internal.operators.FileBasedSPSCQueueMemoryMappedReader.EOFRuntimeException;
import com.github.davidmoten.util.Preconditions;

import rx.Subscription;
import rx.functions.Func0;

public final class FileBasedSPSCQueueMemoryMapped<T> implements Queue<T>, Subscription {

	static final int EOF_MARKER = -1;

	private final Queue<File> inactive = new LinkedList<File>();
	private final Deque<File> active = new ArrayDeque<File>();
	private final Object lock = new Object();
	private final Func0<File> factory;
	private final int size;
	// only needs to be visible to thread calling poll()
	private FileBasedSPSCQueueMemoryMappedReader<T> reader;
	// only needs to be visible to thread calling offer()
	private FileBasedSPSCQueueMemoryMappedWriter<T> writer;
	private final AtomicInteger wip = new AtomicInteger();
	private volatile boolean unsubscribed = false;

	private DataSerializer<T> serializer;

	public FileBasedSPSCQueueMemoryMapped(Func0<File> factory, int size, DataSerializer<T> serializer) {
		Preconditions.checkNotNull(factory);
		Preconditions.checkNotNull(serializer);
		this.factory = factory;
		this.size = size;
		this.serializer = serializer;
		File file = factory.call();
		this.reader = new FileBasedSPSCQueueMemoryMappedReader<T>(file, size, serializer);
		this.writer = new FileBasedSPSCQueueMemoryMappedWriter<T>(file, size, serializer);
		this.active.offer(file);
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
	public boolean offer(T t) {
		// thread safe with poll() and unsubscribe()
		try {
			wip.incrementAndGet();
			if (unsubscribed)
				return true;
			if (!writer.offer(t)) {
				File nextFile;
				synchronized (lock) {
					nextFile = inactive.poll();
					if (nextFile == null) {
						nextFile = factory.call();
					}
					active.offerLast(nextFile);
				}
				writer.close();
				writer = new FileBasedSPSCQueueMemoryMappedWriter<T>(nextFile, size, serializer);
				return writer.offer(t);
			} else {
				return true;
			}
		} finally {
			checkUnsubscribe();
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
	public T poll() {
		// thread safe with offer() and unsubscribe()
		try {
			wip.incrementAndGet();
			if (unsubscribed)
				return null;
			return reader.poll();
		} catch (EOFRuntimeException e) {
			File nextFile;
			synchronized (lock) {
				if (active.size() == 1) {
					return null;
				} else {
					nextFile = active.pollFirst();
				}
			}
			reader.close();
			synchronized (lock) {
				inactive.offer(reader.file());
			}
			reader = new FileBasedSPSCQueueMemoryMappedReader<T>(nextFile, size, serializer);
			return reader.poll();
		} finally {
			checkUnsubscribe();
		}
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
