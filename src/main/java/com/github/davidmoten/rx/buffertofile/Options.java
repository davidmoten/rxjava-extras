package com.github.davidmoten.rx.buffertofile;

import com.github.davidmoten.util.Optional;
import com.github.davidmoten.util.Preconditions;

public final class Options {

    private final CacheType cacheType;
    private final Optional<Integer> cacheSizeItems;
    private final Optional<Double> storageSizeLimitBytes;
    private final boolean delayError;

    private Options(CacheType cacheType, Optional<Integer> cacheSizeItems,
            Optional<Double> storageSizeLimitBytes, boolean delayError) {
        Preconditions.checkNotNull(cacheType);
        Preconditions.checkArgument(!cacheSizeItems.isPresent() || cacheSizeItems.get() > 0,
                "cacheSizeItems cannot be negative or zero");
        Preconditions.checkArgument(
                !storageSizeLimitBytes.isPresent() || storageSizeLimitBytes.get() > 0,
                "storageSizeLimitBytes cannot be negative or zero");
        this.cacheType = cacheType;
        this.cacheSizeItems = cacheSizeItems;
        this.storageSizeLimitBytes = storageSizeLimitBytes;
        this.delayError = delayError;
    }

    public CacheType cacheType() {
        return cacheType;
    }

    public Optional<Integer> cacheSizeItems() {
        return cacheSizeItems;
    }

    public Optional<Double> storageSizeLimitBytes() {
        return storageSizeLimitBytes;
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

    public static Builder cacheType(CacheType cacheType) {
       return builder().cacheType(cacheType);
    }

    public static Builder cacheSizeItems(int cacheSizeItems) {
        return builder().cacheSizeItems(cacheSizeItems);
    }

    public static Builder storageSizeLimitBytes(double storageSizeLimitBytes) {
        return builder().storageSizeLimitBytes(storageSizeLimitBytes);
    }
    
    public static Builder delayError(boolean delayError) {
        return builder().delayError(delayError);
    }
    
    public static class Builder {

        private CacheType cacheType = CacheType.SOFT_REF;
        private Optional<Integer> cacheSizeItems = Optional.absent();
        private Optional<Double> storageSizeLimitBytes = Optional.absent();
        private boolean delayError = true;

        private Builder() {
        }

        public Builder cacheType(CacheType cacheType) {
            this.cacheType = cacheType;
            return this;
        }

        public Builder cacheSizeItems(int cacheSizeItems) {
            this.cacheSizeItems = Optional.of(cacheSizeItems);
            return this;
        }

        public Builder storageSizeLimitBytes(double storageSizeLimitBytes) {
            this.storageSizeLimitBytes = Optional.of(storageSizeLimitBytes);
            return this;
        }
        
        public Builder delayError(boolean delayError) {
            this.delayError = delayError;
            return this;
        }

        public Options build() {
            return new Options(cacheType, cacheSizeItems, storageSizeLimitBytes, delayError);
        }
    }
}