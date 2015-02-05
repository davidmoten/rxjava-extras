package com.github.davidmoten.rx;

import rx.Observable;
import rx.functions.Func1;

public final class Functions {

    private Functions() {
        // do nothing
    }

    public static <T> Func1<T, T> identity() {
        return new Func1<T, T>() {
            @Override
            public T call(T t) {
                return t;
            }
        };
    }

    public static <T> Func1<T, Boolean> alwaysTrue() {
        return new Func1<T, Boolean>() {
            @Override
            public Boolean call(T t) {
                return true;
            }
        };
    }

    public static <T> Func1<T, Boolean> alwaysFalse() {
        return new Func1<T, Boolean>() {
            @Override
            public Boolean call(T t) {
                return false;
            }
        };
    }

    public static <T, R> Func1<T, R> constant(final R r) {
        return new Func1<T, R>() {
            @Override
            public R call(T t) {
                return r;
            }
        };
    }

    public static <T> Func1<T, Boolean> not(final Func1<T, Boolean> f) {
        return new Func1<T, Boolean>() {
            @Override
            public Boolean call(T t) {
                return !f.call(t);
            }
        };
    }

    public static <T> Func1<T, Observable<T>> just() {
        return new Func1<T, Observable<T>>() {
            @Override
            public Observable<T> call(T t) {
                return Observable.just(t);
            }
        };
    }

}
