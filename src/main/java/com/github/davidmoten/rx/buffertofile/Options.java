package com.github.davidmoten.rx.buffertofile;

import com.github.davidmoten.util.Optional;
import com.github.davidmoten.util.Preconditions;

public final class Options {

    public static final int UNLIMITED = 0;

    private final CacheType cacheType;
    private final Optional<Integer> cacheSizeItems;
    private final Optional<Double> storageSizeLimitBytes;

    private Options(CacheType cacheType, Optional<Integer> cacheSizeItems,
            Optional<Double> storageSizeLimitBytes) {
        Preconditions.checkNotNull(cacheType);
        Preconditions.checkArgument(!cacheSizeItems.isPresent() || cacheSizeItems.get() > 0,
                "cacheSizeItems cannot be negative or zero");
        Preconditions.checkArgument(
                !storageSizeLimitBytes.isPresent() || storageSizeLimitBytes.get() > 0,
                "storageSizeLimitBytes cannot be negative or zero");
        this.cacheType = cacheType;
        this.cacheSizeItems = cacheSizeItems;
        this.storageSizeLimitBytes = storageSizeLimitBytes;
    }

    public CacheType getCacheType() {
        return cacheType;
    }

    public Optional<Integer> getCacheSizeItems() {
        return cacheSizeItems;
    }

    public Optional<Double> getStorageSizeLimitBytes() {
        return storageSizeLimitBytes;
    }

    /**
     * Defaults are {@code cacheType=CacheType.SOFT_REF},
     * {@code cacheSizeItems=absent (UNLIMITED)},
     * {@code storageSizeLimitBytes=absent (UNLIMITED)}.
     * 
     * @return a builder object for Options
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private CacheType cacheType = CacheType.SOFT_REF;
        private Optional<Integer> cacheSizeItems = Optional.absent();
        private Optional<Double> storageSizeLimitBytes = Optional.absent();

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

        public Options build() {
            return new Options(cacheType, cacheSizeItems, storageSizeLimitBytes);
        }
    }
}