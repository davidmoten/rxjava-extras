package com.github.davidmoten.rx.internal.operators;

public final class EntryBytes {

    private final Entry entry;
    private final byte[] bytes;

    public EntryBytes(Entry entry, byte[] bytes) {
        this.entry = entry;
        this.bytes = bytes;
    }

}
