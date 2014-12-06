package com.github.davidmoten.rx.util;

public class Optional<T> {

    private final boolean isPresent;
    private final T value;

    private Optional(boolean isPresent, T value) {
        this.isPresent = isPresent;
        this.value = value;
    }

    private static Optional<?> absent = new Optional<Object>(false, (Object) null);

    public static <T> Optional<T> of(T t) {
        return new Optional<T>(true, t);
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> absent() {
        return (Optional<T>) absent;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public T get() {
        if (isPresent)
            return value;
        else
            throw new RuntimeException("cannot call get when value not present");
    }

}
