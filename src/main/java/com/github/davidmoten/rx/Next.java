package com.github.davidmoten.rx;

public interface Next<T> {
    Optional<T> next();
}
