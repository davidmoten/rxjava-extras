package com.github.davidmoten.rx.internal.operators;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.google.common.io.ByteArrayDataOutput;

import rx.Subscription;

public class FileBasedSPSCQueueMemoryMapped<T> implements Queue<T>, Subscription {

	private final MappedByteBuffer write;
	private final MappedByteBuffer read;
	private final DataSerializer<T> serializer;
	private final DataOutput output;
	private final DataInput input;
	private final ByteArrayDataOutput buffer = new ByteArrayDataOutputStream();

	// mutable state
	private long writePosition;

	private static class ByteArrayDataOutputStream implements ByteArrayDataOutput {

		private final ByteArrayOutputStream bytes;
		private final DataOutputStream output;

		ByteArrayDataOutputStream() {
			this.bytes = new ByteArrayOutputStream();
			this.output = new DataOutputStream(bytes);
		}

		@Override
		public void write(int b) {
			try {
				output.write(b);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void write(byte[] b) {
			try {
				output.write(b);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void write(byte[] b, int off, int len) {
			try {
				output.write(b, off, len);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void writeBoolean(boolean v) {
			try {
				output.writeBoolean(v);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void writeByte(int v) {
			try {
				output.writeByte(v);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void writeBytes(String s) {
			try {
				output.writeBytes(s);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void writeChar(int v) {
			try {
				output.writeChar(v);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void writeChars(String s) {
			try {
				output.writeChars(s);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void writeDouble(double v) {
			try {
				output.writeDouble(v);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void writeFloat(float v) {
			try {
				output.writeFloat(v);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void writeInt(int v) {
			try {
				output.writeInt(v);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void writeLong(long v) {
			try {
				output.writeLong(v);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void writeShort(int v) {
			try {
				output.writeShort(v);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public void writeUTF(String s) {
			try {
				output.writeUTF(s);
			} catch (IOException impossible) {
				throw new AssertionError(impossible);
			}
		}

		@Override
		public byte[] toByteArray() {
			return bytes.toByteArray();
		}

	}

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
		output = new DataOutputStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				write.put((byte) b);
			}
		});
		input = new DataInputStream(new InputStream() {
			@Override
			public int read() throws IOException {
				return read.get();
			}
		});

	}

	@Override
	public boolean offer(T t) {
		try {
			serializer.serialize(output, t);
			return true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public T poll() {
		throw new UnsupportedOperationException();
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
