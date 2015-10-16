package com.github.davidmoten.rx;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;

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

    private static class HolderDoNothing2 {
        static final Action2<Object, Object> INSTANCE = new Action2<Object, Object>() {

            @Override
            public void call(Object t, Object t2) {
                // do Nothing
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T, R> Action2<T, R> doNothing2() {
        return (Action2<T, R>) HolderDoNothing2.INSTANCE;
    }

    private static class HolderDoNothing3 {
        static final Action3<Object, Object, Object> INSTANCE = new Action3<Object, Object, Object>() {

            @Override
            public void call(Object t, Object t2, Object t3) {
                // do Nothing
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T, R, S> Action3<T, R, S> doNothing3() {
        return (Action3<T, R, S>) HolderDoNothing3.INSTANCE;
    }

    public static Action0 unsubscribe(final Subscription subscription) {
        return new Action0() {
            @Override
            public void call() {
                subscription.unsubscribe();
            }
        };
    }

    public static <T> Action1<T> increment(final AtomicInteger count) {
        return new Action1<T>() {
            @Override
            public void call(T t) {
                count.incrementAndGet();
            }
        };
    }

    public static <T> Action1<T> decrement(final AtomicInteger count) {
        return new Action1<T>() {
            @Override
            public void call(T t) {
                count.incrementAndGet();
            }
        };
    }

    public static <T> Action1<T> println() {
        return new Action1<T>() {
            @Override
            public void call(T t) {
                System.out.println(t);
            }
        };
    }

    public static <T> Action1<T> setToTrue(final AtomicBoolean ref) {
        return new Action1<T>() {

            @Override
            public void call(T t) {
                ref.set(true);
            }
        };
    }

}
