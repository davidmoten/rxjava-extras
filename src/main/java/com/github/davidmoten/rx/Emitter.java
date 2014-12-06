package com.github.davidmoten.rx;

public interface Emitter {

    void emitAll();

    void emitOne();

    boolean completed();
}
