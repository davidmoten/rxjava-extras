package com.github.davidmoten.rx.buffertofile;

import java.io.File;
import java.io.IOException;

import com.github.davidmoten.util.Preconditions;

import rx.functions.Func0;

public final class Options {

	public static final String DEFAULT_FILE_PREFIX = "bufferToFileDb";

	private final Func0<File> fileFactory;
	private final boolean delayError;
	private final long rolloverEvery;
	private final long rolloverSizeBytes;
	private final int bufferSizeBytes;

	private Options(Func0<File> filefactory, boolean delayError, long rolloverEvery, int bufferSizeBytes,
			long rolloverSizeBytes) {
		Preconditions.checkNotNull(filefactory);
		Preconditions.checkArgument(rolloverSizeBytes > 0, "rolloverSizeBytes must be greater than zero");
		Preconditions.checkArgument(rolloverEvery > 1, "rolloverEvery must be greater than one");
		Preconditions.checkArgument(bufferSizeBytes > 0, "bufferSizeBytes must be greater than 0");
		this.fileFactory = filefactory;
		this.delayError = delayError;
		this.rolloverEvery = rolloverEvery;
		this.rolloverSizeBytes = rolloverSizeBytes;
		this.bufferSizeBytes = bufferSizeBytes;
	}

	public Func0<File> fileFactory() {
		return fileFactory;
	}

	public boolean delayError() {
		return delayError;
	}

	public long rolloverEvery() {
		return rolloverEvery;
	}

	public long rolloverSizeBytes() {
		return rolloverSizeBytes;
	}

	public int bufferSizeBytes() {
		return bufferSizeBytes;
	}

	public boolean rolloverEnabled() {
		return rolloverSizeBytes != Long.MAX_VALUE || rolloverEvery != Long.MAX_VALUE;
	}

	/**
	 * Defaults are {@code cacheType=CacheType.SOFT_REF},
	 * {@code cacheSizeItems=absent (UNLIMITED)},
	 * {@code storageSizeLimitBytes=absent (UNLIMITED)}.
	 * 
	 * @return a builder object for Options
	 */
	private static Builder builder() {
		return new Builder();
	}

	public static Builder fileFactory(Func0<File> fileFactory) {
		return builder().fileFactory(fileFactory);
	}

	public static Builder delayError(boolean delayError) {
		return builder().delayError(delayError);
	}

	public static Builder rolloverEvery(long rolloverEvery) {
		return builder().rolloverEvery(rolloverEvery);
	}

	public static Builder rolloverSizeBytes(long rolloverSizeBytes) {
		return builder().rolloverSizeBytes(rolloverSizeBytes);
	}

	public static Builder rolloverSizeMB(double rolloverSizeMB) {
		return builder().rolloverSizeMB(rolloverSizeMB);
	}

	public static Builder disableRollover() {
		return builder().disableRollover();
	}

	public static Builder bufferSizeBytes(int bufferSizeBytes) {
		return builder().bufferSizeBytes(bufferSizeBytes);
	}

	public static Options defaultInstance() {
		return builder().build();
	}

	public static class Builder {

		private Func0<File> fileFactory = FileFactoryHolder.INSTANCE;
		private boolean delayError = true;
		private long rolloverSizeBytes = Long.MAX_VALUE;
		private long rolloverEvery = 1000000;
		private int bufferSizeBytes = 1024;

		private Builder() {
		}

		public Builder rolloverSizeMB(double rolloverSizeMB) {
			return rolloverSizeBytes(Math.round(rolloverSizeMB * 1024 * 1024));
		}

		public Builder rolloverSizeBytes(long rolloverSizeBytes) {
			this.rolloverSizeBytes = rolloverSizeBytes;
			return this;
		}

		public Builder disableRollover() {
			this.rolloverSizeBytes = Long.MAX_VALUE;
			return rolloverEvery(Long.MAX_VALUE);
		}

		public Builder bufferSizeBytes(int bufferSizeBytes) {
			this.bufferSizeBytes = bufferSizeBytes;
			return this;
		}

		public Builder rolloverEvery(long rolloverEvery) {
			this.rolloverEvery = rolloverEvery;
			return this;
		}

		/**
		 * Sets the file factory to be used by the queue storage mechanism.
		 * Defaults to using {@code File.createTempFile("bufferToFileDb","")} if
		 * this method is not called.
		 * 
		 * @param fileFactory
		 *            the factory
		 * @return the current builder
		 */
		public Builder fileFactory(Func0<File> fileFactory) {
			this.fileFactory = fileFactory;
			return this;
		}

		/**
		 * Sets if errors are delayed or not when detected. Defaults to
		 * {@code false} if this method not called.
		 * 
		 * @param delayError
		 *            if true errors do not shortcut the queue.
		 * @return the current builder
		 */
		public Builder delayError(boolean delayError) {
			this.delayError = delayError;
			return this;
		}

		public Options build() {
			return new Options(fileFactory, delayError, rolloverEvery, bufferSizeBytes, rolloverSizeBytes);
		}
	}

	private static class FileFactoryHolder {

		private static final Func0<File> INSTANCE = new Func0<File>() {
			@Override
			public File call() {
				try {
					return File.createTempFile(DEFAULT_FILE_PREFIX, "");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

}