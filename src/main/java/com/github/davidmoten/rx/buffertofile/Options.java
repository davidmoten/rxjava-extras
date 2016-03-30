package com.github.davidmoten.rx.buffertofile;

import com.github.davidmoten.util.Preconditions;

public final class Options {

    public static final int UNLIMITED = 0;

    private final CacheType cacheType;
    private final int cacheSizeItems;
    private final double storageSizeLimitBytes;

    private Options(CacheType cacheType, int cacheSizeItems, double storageSizeLimitBytes) {
        Preconditions.checkNotNull(cacheType);
        Preconditions.checkArgument(cacheSizeItems >= 0, "cacheSizeItems cannot be negative");
        Preconditions.checkArgument(storageSizeLimitBytes >= 0,
                "storageSizeLimitBytes cannot be negative");
        this.cacheType = cacheType;
        this.cacheSizeItems = cacheSizeItems;
        this.storageSizeLimitBytes = storageSizeLimitBytes;
    }

    public CacheType getCacheType() {
        return cacheType;
    }

    public int getCacheSizeItems() {
        return cacheSizeItems;
    }

    public double getStorageSizeLimitBytes() {
        return storageSizeLimitBytes;
    }

    /**
     * Defaults are {@code cacheType=CacheType.NO_CACHE},
     * {@code cacheSizeItems=Options.UNLIMITED},
     * {@code storageSizeLimitBytes=Options.UNLIMITED}.
     * 
     * @return a builder object for Options
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private CacheType cacheType = CacheType.NO_CACHE;
        private int cacheSizeItems = UNLIMITED;
        private double storageSizeLimitBytes = UNLIMITED;

        private Builder() {
        }

        public Builder cacheType(CacheType cacheType) {
            this.cacheType = cacheType;
            return this;
        }

        public Builder cacheSizeItems(int cacheSizeItems) {
            this.cacheSizeItems = cacheSizeItems;
            return this;
        }

        public Builder storageSizeLimitBytes(double storageSizeLimitBytes) {
            this.storageSizeLimitBytes = storageSizeLimitBytes;
            return this;
        }

        public Options build() {
            return new Options(cacheType, cacheSizeItems, storageSizeLimitBytes);
        }
    }
}