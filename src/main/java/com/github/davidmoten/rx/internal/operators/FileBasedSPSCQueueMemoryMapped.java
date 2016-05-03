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

import com.github.davidmoten.rx.buffertofile.DataSerializer;

import rx.Subscription;

public class FileBasedSPSCQueueMemoryMapped<T> implements Subscription {

	private final MappedByteBuffer write;
	private final MappedByteBuffer read;
	private final DataSerializer<T> serializer;
	private final DataOutput output;
	private final DataInput input;

	public FileBasedSPSCQueueMemoryMapped(File file, int size, DataSerializer<T> serializer) {
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
	private static final byte EOF_MARKER = -1;
	private static final byte NONE_AVAILABLE_MARKER = 0;

	public boolean offer(T t, int serializedLength) {
		if (serializedLength + 4 > write.remaining()) {
			write.putInt(EOF_MARKER);
			nextWriteFile();
		}
		int position = write.position();
		try {
			serializer.serialize(output, t);
			write.putInt(NONE_AVAILABLE_MARKER);
			int length = write.position() - position - 4;
			write.position(position - 4);
			output.writeInt(length);
			return true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void nextWriteFile() {
		// TODO Auto-generated method stub

	}

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
	public void unsubscribe() {
		// TODO Auto-generated method stub
		
	}

}
