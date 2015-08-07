package com.github.davidmoten.rx;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.util.concurrent.AtomicDouble;

import rx.functions.Action1;

public final class Actions {

    public static Action1<Integer> setAtomic(final AtomicInteger a) {
        return new Action1<Integer>() {

            @Override
            public void call(Integer t) {
                a.set(t);
            }
        };
    }

    public static Action1<Double> setAtomic(final AtomicDouble a) {
        return new Action1<Double>() {

            @Override
            public void call(Double t) {
                a.set(t);
            }
        };
    }

    public static Action1<Boolean> setAtomic(final AtomicBoolean a) {
        return new Action1<Boolean>() {

            @Override
            public void call(Boolean t) {
                a.set(t);
            }
        };
    }

    public static <T> Action1<T> setAtomic(final AtomicReference<T> a) {
        return new Action1<T>() {

            @Override
            public void call(T t) {
                a.set(t);
            }
        };
    }
}
