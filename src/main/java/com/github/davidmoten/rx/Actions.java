package com.github.davidmoten.rx;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import rx.functions.Action0;
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

    public static Action1<Long> setAtomic(final AtomicLong a) {
        return new Action1<Long>() {

            @Override
            public void call(Long t) {
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

    private static class HolderDoNothing0 {
        static final Action0 INSTANCE = new Action0() {

            @Override
            public void call() {
                // do nothing
            }
        };

    }

    public static Action0 doNothing0() {
        return HolderDoNothing0.INSTANCE;
    }

    private static class HolderDoNothing1 {
        static final Action1<Object> INSTANCE = new Action1<Object>() {

            @Override
            public void call(Object t) {
                // do Nothing
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> Action1<T> doNothing1() {
        return (Action1<T>) HolderDoNothing1.INSTANCE;
    }
}
