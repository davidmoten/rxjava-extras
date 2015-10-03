package com.github.davidmoten.rx.util;

import rx.Observable;

public enum BackpressureStrategy {
    /**
     * Corresponds to {@link Observable#onBackpressureBuffer}.
     */
    BUFFER,

    /**
     * Corresponds to {@link Observable#onBackpressureDrop}.
     */
    DROP,

    /**
     * Corresponds to {@link Observable#onBackpressureLatest}.
     */
    LATEST;
}