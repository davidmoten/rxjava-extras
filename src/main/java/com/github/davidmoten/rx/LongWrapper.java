package com.github.davidmoten.rx;

//not threadsafe
public class LongWrapper {
    private long value;

    public LongWrapper(long value) {
        this.value = value;
    }

    public long get() {
        return value;
    }

    public long decrease(long d) {
        value -= d;
        return value;
    }
}