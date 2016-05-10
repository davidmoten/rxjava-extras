package com.github.davidmoten.rx.internal.operators;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.util.ByteArrayOutputStreamNoCopyUnsynchronized;
import com.github.davidmoten.util.Preconditions;

public class FileBasedSPSCQueueMemoryMappedReaderWriter<T> {

	private volatile RandomAccessFile f;
	private volatile FileChannel channel;
	private DataInputStream input;
	private DataOutputStream output;
	private MappedByteBuffer read;
	private MappedByteBuffer write;
	private final DataSerializer<T> serializer;
	private final File file;
	private final int fileSize;
	private final DataOutput buffer;
	// TODO can be passed in to constructor for reuse
	private final ByteArrayOutputStreamNoCopyUnsynchronized bytes;
	private final AtomicInteger status = new AtomicInteger(WRITTEN_READ);
	private final Object markerLock = new Object();

	static final int WRITTEN_READ = 0;
	static final int WRITTEN_READ_NOT_STARTED = 1;
	static final int WRITTEN_READING = 2;
	static final int WRITING_NOT_READING = 3;
	static final int WRITING_READING = 4;

	public FileBasedSPSCQueueMemoryMappedReaderWriter(File file, int fileSize, DataSerializer<T> serializer) {
		Preconditions.checkArgument(serializer.size() == 0 || serializer.size() <= fileSize - 2 * MARKER_HEADER_SIZE,
				"serializer.size() must be less than or equal to file based queue size - 2");
		this.file = file;
		this.serializer = serializer;
		this.fileSize = fileSize;
		this.bytes = new ByteArrayOutputStreamNoCopyUnsynchronized();
		this.buffer = new DataOutputStream(bytes);
	}

	public FileBasedSPSCQueueMemoryMappedReaderWriter<T> openForRead() {
		// System.out.println("openForRead " + file);

		if (!status.compareAndSet(WRITTEN_READ_NOT_STARTED, WRITTEN_READING))
			status.compareAndSet(WRITING_NOT_READING, WRITING_READING);
		while (true) {
			int st = status.get();
			int newStatus;
			if (st == WRITTEN_READ_NOT_STARTED)
				newStatus = WRITTEN_READING;
			else if (st == WRITING_NOT_READING)
				newStatus = WRITING_READING;
			else
				newStatus = st;
			if (status.compareAndSet(st, newStatus)) {
				checkClose(newStatus);
				break;
			}
		}
		try {
			if (f == null) {
				f = new RandomAccessFile(file, "r");
			}
			if (channel == null) {
				channel = f.getChannel();
			}
			read = channel.map(MapMode.READ_ONLY, 0, channel.size());
			input = new DataInputStream(new MappedByteBufferInputStream(read));
			// System.out.println("opened for read " + file.getName());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public void closeForRead() {
		// System.out.println("closeForRead " + file);
		while (true) {
			int st = status.get();
			int newStatus;
			if (st == WRITTEN_READING)
				newStatus = WRITTEN_READ;
			else
				newStatus = st;
			if (status.compareAndSet(st, newStatus)) {
				checkClose(newStatus);
				break;
			}
		}
	}

	public FileBasedSPSCQueueMemoryMappedReaderWriter<T> openForWrite() {
		// System.out.println("openForWrite " + file);
		while (true) {
			int st = status.get();
			int newStatus;
			if (st == WRITTEN_READ)
				newStatus = WRITING_NOT_READING;
			else
				newStatus = st;
			if (status.compareAndSet(st, newStatus)) {
				checkClose(newStatus);
				break;
			}
		}
		try {
			if (f == null) {
				f = new RandomAccessFile(file, "rw");
			}
			if (channel == null) {
				channel = f.getChannel();
			}
			write = channel.map(MapMode.READ_WRITE, 0, fileSize);
			output = new DataOutputStream(new MappedByteBufferOutputStream(write));
			synchronized (markerLock) {
				output.write(MARKER_END_OF_QUEUE);
			}
			// System.out.println("opened for write " + file.getName());
			return this;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void closeForWrite() {
		// System.out.println("closeForWrite " + file);
		while (true) {
			int st = status.get();
			int newStatus;
			if (st == WRITING_READING)
				newStatus = WRITTEN_READING;
			else if (st == WRITING_NOT_READING)
				newStatus = WRITTEN_READ_NOT_STARTED;
			newStatus = st;
			if (status.compareAndSet(st, newStatus)) {
				checkClose(newStatus);
				break;
			}
		}

	}

	private void checkClose(int newStatus) {
		// System.out.println("close status = " + newStatus + " for " +
		// file.getName());
		if (newStatus == WRITTEN_READ) {
			try {
				channel.close();
				channel = null;
				read = null;
				input = null;
				f.close();
				f = null;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static class MappedByteBufferOutputStream extends OutputStream {

		private final MappedByteBuffer write;

		MappedByteBufferOutputStream(MappedByteBuffer write) {
			this.write = write;
		}

		@Override
		public void write(int b) throws IOException {
			write.put((byte) b);
		}
	}

	private static class MappedByteBufferInputStream extends InputStream {

		private final MappedByteBuffer read;

		MappedByteBufferInputStream(MappedByteBuffer read) {
			this.read = read;
		}

		@Override
		public int read() throws IOException {
			return toUnsignedInteger(read.get());
		}

	}

	private static int toUnsignedInteger(byte b) {
		return b & 0x000000FF;
	}

	private static final EOFRuntimeException EOF = new EOFRuntimeException();

	static final class EOFRuntimeException extends RuntimeException {

		private static final long serialVersionUID = -6943467453336359472L;

	}

	// markers must be powers of 2 so that we can detect
	// a partial write of the byte (which we will treat as END_OF_QUEUE)
	static final byte MARKER_END_OF_QUEUE = 0;
	static final byte MARKER_END_OF_FILE = 1;
	static final byte MARKER_ITEM_PRESENT = 2;
	static final int MARKER_HEADER_SIZE = 1;
	static final int UNKNOWN_LENGTH = 0;

	public T poll() {
		int position = read.position();
		byte marker;
		synchronized (markerLock) {
			marker = read.get();
		}
		if (marker == MARKER_END_OF_QUEUE) {
			read.position(position);
			return null;
		} else if (marker == MARKER_END_OF_FILE) {
			throw EOF;
		} else if (marker == MARKER_ITEM_PRESENT) {
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
		} else {
			throw new RuntimeException("unexpected");
		}
	}

	/**
	 * Returns true if value written to file or false if not enough space
	 * (writes and end-of-file marker in the fixed-length memory mapped file).
	 * 
	 * @param t
	 *            value to write to the serialized queue
	 * @return true if written, false if not enough space
	 */
	public boolean offer(T t) {
		// the current position will be just past the length bytes for this
		// item (length bytes will be 0 at the moment)
		int serializedLength = serializer.size();
		if (serializedLength == UNKNOWN_LENGTH) {
			return offerUnknownLength(t);
		} else {
			return offerKnownLength(t, serializedLength);
		}
	}

	private boolean offerKnownLength(T t, int serializedLength) {
		try {
			if (notEnoughSpace(serializedLength)) {
				markFileAsCompletedAndClose();
				return false;
			}
			int position = write.position();
			// serialize the object t to the file
			serializer.serialize(output, t);
			int length = write.position() - position;
			checkLength(serializedLength, length);
			updateMarkers(serializedLength);
			return true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean offerUnknownLength(T t) {
		try {
			bytes.reset();
			// serialize to an in-memory buffer to calculate length
			serializer.serialize(buffer, t);
			int serializedLength = bytes.size();
			if (notEnoughSpace(serializedLength)) {
				markFileAsCompletedAndClose();
				return false;
			} else {
				write.put(bytes.toByteArrayNoCopy(), 0, bytes.size());
				updateMarkers(serializedLength);
				return true;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkLength(int serializedLength, int length) {
		if (length > serializedLength) {
			throw new IllegalArgumentException(
					"serialized length of value being offered to file queue was greater than serializer.size() value (which was non-zero)");
		}
	}

	private void markFileAsCompletedAndClose() {
		write.position(write.position() - MARKER_HEADER_SIZE);
		synchronized (markerLock) {
			write.put(MARKER_END_OF_FILE);
		}
		closeForWrite();
	}

	private boolean notEnoughSpace(int serializedLength) {
		if (serializedLength > fileSize - 2 * MARKER_HEADER_SIZE)
			throw new RuntimeException("serialized length is larger than can fit in one file");
		return serializedLength + MARKER_HEADER_SIZE > write.remaining();
	}

	private void updateMarkers(int serializedLength) throws IOException {
		// write the marker for the next item
		write.put(MARKER_END_OF_QUEUE);
		// remember the position where the next write starts
		int newWritePosition = write.position();
		// rewind and update the length for the current item
		write.position(write.position() - serializedLength - 2 * MARKER_HEADER_SIZE);
		// now indicate to the reader that it can read this item
		synchronized (markerLock) {
			write.put(MARKER_ITEM_PRESENT);
		}
		// and update the position to the write position for the
		// next item
		write.position(newWritePosition);
	}

	public void close() {
		try {
			f.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
