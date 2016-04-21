package com.github.davidmoten.rx.internal.operators;

public final class NullSentinel {

	private NullSentinel() {
		// prevent instantiation
	}

	private static final Object NULL_SENTINEL = new Object();

	public static boolean isNullSentinel(Object o) {
		return o == NULL_SENTINEL;
	}

	@SuppressWarnings("unchecked")
	public static <T> T instance() {
		return (T) NULL_SENTINEL;
	}

}
