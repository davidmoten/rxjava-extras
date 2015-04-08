package com.github.davidmoten.rx;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func1;
import rx.functions.Func2;

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

    /**
     * Returns a Func2 that adds numbers. Useful for Observable.reduce but not
     * particularly performant as it does instanceOf checks.
     * 
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

    public static <T extends Number> Func2<Statistics, T, Statistics> collectStatsFunction() {
        return new Func2<Statistics, T, Statistics>() {

            @Override
            public Statistics call(Statistics s, T t) {
                return s.add(t);
            }
        };
    }

    public static <T extends Number> Transformer<T, Statistics> collectStats() {
        return new Transformer<T, Statistics>() {

            @Override
            public Observable<Statistics> call(Observable<T> o) {
                return o.scan(Statistics.create(), collectStatsFunction());
            }
        };
    }

    public static class Statistics {
        private final long count;
        private final double sumX;
        private final double sumX2;

        private Statistics(long count, double sumX, double sumX2) {
            this.count = count;
            this.sumX = sumX;
            this.sumX2 = sumX2;
        }

        public static Statistics create() {
            return new Statistics(0, 0, 0);
        }

        public Statistics add(Number number) {
            double x = number.doubleValue();
            return new Statistics(count + 1, sumX + x, sumX2 + x * x);
        }

        public long count() {
            return count;
        }

        public double sum() {
            return sumX;
        }

        public double sumSquares() {
            return sumX2;
        }

        public double mean() {
            return sumX / count;
        }

        public double sd() {
            double m = mean();
            return Math.sqrt(sumX2 / count - m * m);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Statistics [count=");
            builder.append(count);
            builder.append(", sum=");
            builder.append(sum());
            builder.append(", mean=");
            builder.append(mean());
            builder.append(", sd=");
            builder.append(sd());
            builder.append("]");
            return builder.toString();
        }
    }

}
