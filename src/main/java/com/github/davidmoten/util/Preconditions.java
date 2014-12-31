package com.github.davidmoten.util;

public final class Preconditions {

    public static void checkNotNull(Object o) {
        if (o == null)
            throw new NullPointerException();
    }

}
