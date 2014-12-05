package com.github.davidmoten.rx;

public interface Emitter {

    void emitAll();

    void emitSome(LongWrapper numToEmit);
}
