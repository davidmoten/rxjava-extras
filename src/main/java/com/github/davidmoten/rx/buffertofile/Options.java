package com.github.davidmoten.rx.buffertofile;

import java.io.File;
import java.io.IOException;

import com.github.davidmoten.util.Optional;
import com.github.davidmoten.util.Preconditions;

import rx.functions.Func0;

public final class Options {

    private final Func0<File> fileFactory;
    private final CacheType cacheType;
    private final Optional<Integer> cacheSizeItems;
    private final Optional<Double> storageSizeLimitMB;
    private final boolean delayError;

    private Options(Func0<File> filefactory, CacheType cacheType, Optional<Integer> cacheSizeItems,
            Optional<Double> storageSizeLimitMB, boolean delayError) {
        Preconditions.checkNotNull(filefactory);
        Preconditions.checkNotNull(cacheType);
        Preconditions.checkArgument(!cacheSizeItems.isPresent() || cacheSizeItems.get() > 0,
                "cacheSizeItems cannot be negative or zero");
        Preconditions.checkArgument(
                !storageSizeLimitMB.isPresent() || storageSizeLimitMB.get() > 0,
                "storageSizeLimitBytes cannot be negative or zero");
        this.fileFactory = filefactory;
        this.cacheType = cacheType;
        this.cacheSizeItems = cacheSizeItems;
        this.storageSizeLimitMB = storageSizeLimitMB;
        this.delayError = delayError;
    }

    public Func0<File> fileFactory() {
        return fileFactory;
    }

    public CacheType cacheType() {
        return cacheType;
    }

    public Optional<Integer> cacheSizeItems() {
        return cacheSizeItems;
    }

    public Optional<Double> storageSizeLimitMB() {
        return storageSizeLimitMB;
    }

    public boolean delayError() {
        return delayError;
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

    public static Builder cacheType(CacheType cacheType) {
        return builder().cacheType(cacheType);
    }

    public static Builder cacheSizeItems(int cacheSizeItems) {
        return builder().cacheSizeItems(cacheSizeItems);
    }

    public static Builder storageSizeLimitMB(double storageSizeLimitMB) {
        return builder().storageSizeLimitMB(storageSizeLimitMB);
    }

    public static Builder delayError(boolean delayError) {
        return builder().delayError(delayError);
    }
    
    public static Options defaultInstance() {
        return builder().build();
    }

    public static class Builder {

        private Func0<File> fileFactory = FileFactoryHolder.INSTANCE;
        private CacheType cacheType = CacheType.NO_CACHE;
        private Optional<Integer> cacheSizeItems = Optional.absent();
        private Optional<Double> storageSizeLimitMB = Optional.absent();
        private boolean delayError = true;

        private Builder() {
        }

        /**
         * Sets the file factory to be used by the queue storage mechanism.
         * Defaults to using {@code File.createTempFile("bufferToFileDb","")} if
         * this method is not called. MapDB for instance creates two files, one
         * without extension and the other a {@code .p} file.
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
         * Sets the cache type used by the queue storage mechanism. Defaults to
         * {@code CacheType.NO_CACHE} if this method is not called.
         * 
         * @param cacheType
         * @return the current builder
         */
        public Builder cacheType(CacheType cacheType) {
            this.cacheType = cacheType;
            return this;
        }

        public Builder cacheSizeItems(int cacheSizeItems) {
            this.cacheSizeItems = Optional.of(cacheSizeItems);
            return this;
        }

        public Builder storageSizeLimitMB(double storageSizeLimitMB) {
            this.storageSizeLimitMB = Optional.of(storageSizeLimitMB);
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
            return new Options(fileFactory, cacheType, cacheSizeItems, storageSizeLimitMB,
                    delayError);
        }
    }

    private static class FileFactoryHolder {
        private static final Func0<File> INSTANCE = new Func0<File>() {
            @Override
            public File call() {
                try {
                    return File.createTempFile("bufferToFileDb", "");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}