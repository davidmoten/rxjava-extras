package com.github.davidmoten.util;

import rx.Observable;

public class Optional<T> {

    private final T value;
    private final boolean present;

    private Optional(T value, boolean present) {
        this.value = value;
        this.present = present;
    }

    public boolean isPresent() {
        return present;
    }

    public T get() {
        if (present)
            return value;
        else
            throw new NotPresentException();
    }

    public T or(T alternative) {
        if (present)
            return value;
        else
            return alternative;
    }

    public Observable<T> toObservable() {
        if (present)
            return Observable.just(value);
        else
            return Observable.empty();
    }

    public static <T> Optional<T> fromNullable(T t) {
        if (t == null)
            return Optional.absent();
        else
            return Optional.of(t);
    }

    public static <T> Optional<T> of(T t) {
        return new Optional<T>(t, true);
    }

    public static <T> Optional<T> absent() {
        return new Optional<T>(null, false);
    }

    public static class NotPresentException extends RuntimeException {

        private static final long serialVersionUID = -4444814681271790328L;

    }
}