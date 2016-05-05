package com.github.davidmoten.rx.internal.operators;

import static com.github.davidmoten.rx.internal.operators.FileBasedSPSCQueueMemoryMapped.EOF_MARKER;

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

public class FileBasedSPSCQueueMemoryMappedReaderWriter<T> {

	private volatile RandomAccessFile f;
	private volatile FileChannel channel;
	private DataInputStream input;
	private DataOutputStream output;
	private MappedByteBuffer read;
	private MappedByteBuffer write;
	private final DataSerializer<T> serializer;
	private final File file;
	private int fileSize;
	private final DataOutput buffer;
	//TODO can be passed in to constructor for reuse
    private final ByteArrayOutputStreamNoCopyUnsynchronized bytes;
	private volatile AtomicInteger status = new AtomicInteger(0); // 0 = closed
																	// for both,
																	// 1 = open
																	// for
																	// reading,
																	// 2 = open
																	// for
																	// writing,
																	// 3 = open
																	// for both
	

	public FileBasedSPSCQueueMemoryMappedReaderWriter(File file, int fileSize, DataSerializer<T> serializer) {
		this.file = file;
		this.serializer = serializer;
		this.fileSize = fileSize;
		this.bytes = new ByteArrayOutputStreamNoCopyUnsynchronized();
        this.buffer = new DataOutputStream(bytes);
	}

	public FileBasedSPSCQueueMemoryMappedReaderWriter<T> openForRead() {
		while (true) {
			int st = status.get();
			int newStatus = st ^ 1;
			if (status.compareAndSet(st, newStatus))
				break;
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
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public void closeForRead() {
		while (true) {
			int st = status.get();
			int newStatus = st ^ 1;
			if (status.compareAndSet(st, newStatus)) {
				checkClose(newStatus);
				break;
			}
		}
	}
	
	public FileBasedSPSCQueueMemoryMappedReaderWriter<T> openForWrite() {
		try {
			if (f == null) {
				f = new RandomAccessFile(file, "rw");
			}
			if (channel == null) {
				channel = f.getChannel();
			}
			write = channel.map(MapMode.READ_WRITE, 0, fileSize);
			output = new DataOutputStream(new MappedByteBufferOutputStream(write));
			return this;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void closeForWrite() {
		while (true) {
			int st = status.get();
			int newStatus = st ^ 2;
			if (status.compareAndSet(st, newStatus)) {
				checkClose(newStatus);
				break;
			}
		}
	}
	
	private void checkClose(int newStatus) {
		if (newStatus == 0) {
			try {
				channel.close();
				channel = null;
				read = null;
				input = null;
				f.close();
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
			return read.get();
		}

	}

	private static final EOFRuntimeException EOF = new EOFRuntimeException();

	static final class EOFRuntimeException extends RuntimeException {

		private static final long serialVersionUID = -6943467453336359472L;

	}

	public T poll() {
		read.mark();
		int length = read.getInt();
		if (length == FileBasedSPSCQueueMemoryMapped.EOF_MARKER) {
			throw EOF;
		} else if (length == 0) {
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
	
	public boolean offer(T t) {
		// the current position will be just past the length bytes for this
		// item (length bytes will be 0 at the moment)
		int serializedLength = serializer.size();
		if (serializedLength == 0) {
			try {
				bytes.reset();
				// serialize to an in-memory buffer to calculate length
				serializer.serialize(buffer, t);
				if (bytes.size() + 4 > write.remaining()) {
					write.putInt(EOF_MARKER);
					close();
					return false;
				} else {
					write.put(bytes.toByteArrayNoCopy(), 0, bytes.size());
					write.putInt(0);
					// remember the position
					int newWritePosition = write.position();
					// rewind and update the length for the current item
					write.position(write.position() - bytes.size() - 8);
					// now indicate to the reader that it can read this item
					output.writeInt(bytes.size());
					// and update the position to the write position for the
					// next item
					write.position(newWritePosition);
					return true;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else if (serializedLength + 4 > write.remaining()) {
			write.putInt(EOF_MARKER);
			close();
			return false;
		} else {
			int position = write.position();
			try {
				// serialize the object t to the file
				serializer.serialize(output, t);
				int length = write.position() - position;
				if (length > serializedLength) {
					throw new IllegalArgumentException("serialized length of t was greater than serializedLength");
				}
				// write a length of zero for the next item
				write.putInt(0);
				// remember the position
				int newWritePosition = write.position();
				// rewind and update the length for the current item
				write.position(position - 4);
				// now indicate to the reader that it can read this item
				// because the length will now be non-zero
				output.writeInt(length);
				// and update the position to the write position for the next
				// item
				write.position(newWritePosition);
				return true;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void close() {
		try {
			f.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
