package com.github.davidmoten.util;

public class ErrorAndDuration {
    
    private final Throwable throwable;
    private final long durationMs;

    public ErrorAndDuration(Throwable throwable, long durationMs) {
        this.throwable = throwable;
        this.durationMs = durationMs;
    }

    public Throwable throwable() {
        return throwable;
    }

    public long durationMs() {
        return durationMs;
    }

}