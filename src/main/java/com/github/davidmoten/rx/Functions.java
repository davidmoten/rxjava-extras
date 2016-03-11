package com.github.davidmoten.rx;

import java.util.Comparator;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

public final class Functions {

    private Functions() {
        // prevent instantiation
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

    public static <T> Func0<T> constant0(final T t) {
        return new Func0<T>() {

            @Override
            public T call() {
                return t;
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

    /**
     * Returns a Func2 that adds numbers. Useful for Observable.reduce but not
     * particularly performant as it does instanceOf checks.
     * 
     * @param <T>
     *            generic type of Number being added
     * @return Func2 that adds numbers
     */
    public static <T extends Number> Func2<T, T, T> add() {
        return new Func2<T, T, T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T call(T a, T b) {
                if (a instanceof Integer)
                    return (T) (Number) (a.intValue() + b.intValue());
                else if (a instanceof Long)
                    return (T) (Number) (a.longValue() + b.longValue());
                else if (a instanceof Double)
                    return (T) (Number) (a.doubleValue() + b.doubleValue());
                else if (a instanceof Float)
                    return (T) (Number) (a.floatValue() + b.floatValue());
                else if (a instanceof Byte)
                    return (T) (Number) (a.byteValue() + b.byteValue());
                else if (a instanceof Short)
                    return (T) (Number) (a.shortValue() + b.shortValue());
                else
                    throw new RuntimeException("not implemented");
            }
        };
    }

    public static <T extends Number> Func2<Statistics, T, Statistics> collectStats() {
        return new Func2<Statistics, T, Statistics>() {

            @Override
            public Statistics call(Statistics s, T t) {
                return s.add(t);
            }
        };
    }

    public static <T> Func2<T, T, Integer> toFunc2(final Comparator<? super T> comparator) {
        return new Func2<T, T, Integer>() {
            @Override
            public Integer call(T t1, T t2) {
                return comparator.compare(t1, t2);
            }
        };
    }

    public static <T> Comparator<T> toComparator(final Func2<? super T, ? super T, Integer> f) {
        return new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                return f.call(a, b);
            }
        };
    }

    public static <T, R> Func2<T, R, Boolean> alwaysTrue2() {
        return new Func2<T, R, Boolean>() {

            @Override
            public Boolean call(T t1, R t2) {
                return true;
            }
        };
    }

    public static <T, R> Func2<T, R, Boolean> alwaysFalse2() {
        return new Func2<T, R, Boolean>() {

            @Override
            public Boolean call(T t1, R t2) {
                return false;
            }
        };
    }

    public static <T> Func1<T, Boolean> isNull() {
        return new Func1<T, Boolean>() {

            @Override
            public Boolean call(T t) {
                return t == null;
            }
        };
    }

    public static <T> Func1<T, Boolean> isNotNull() {
        return new Func1<T, Boolean>() {

            @Override
            public Boolean call(T t) {
                return t != null;
            }
        };
    }

}
