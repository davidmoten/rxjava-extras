package com.github.davidmoten.rx;

public interface Emitter<T, R> {

    void emitAll();

    void emitSome(LongWrapper numToEmit);

    boolean noMoreToEmit();

}
