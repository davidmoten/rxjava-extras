package com.github.davidmoten.util;

public final class Preconditions {

    public static void checkNotNull(Object o) {
        checkNotNull(o, null);
    }

    public static void checkNotNull(Object o, String message) {
        if (o == null)
            throw new NullPointerException(message);
    }

    public static void checkArgument(boolean b, String message) {
        if (!b)
            throw new IllegalArgumentException(message);
    }

}
