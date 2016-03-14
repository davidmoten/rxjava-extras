package com.github.davidmoten.rx;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

public final class Maths {

    private Maths() {
        // prevent instantiation
    }

    public static Observable<Double> solveWithNewtonsMethod(final Func1<Double, Double> f,
            final Func1<Double, Double> dfdx, double x0) {
        return Observable.just(1).repeat().scan(x0, new Func2<Double, Integer, Double>() {
            @Override
            public Double call(Double xn, Integer n) {
                return xn - f.call(xn) / dfdx.call(xn);
            }
        });
    }

    public static Observable<Double> solveWithNewtonsMethod(final Func1<Double, Double> f,
            final double x0, final double h) {
        Func1<Double, Double> dfdx = new Func1<Double, Double>() {
            @Override
            public Double call(Double x) {
                return (f.call(x + h) - f.call(x - h)) / 2.0 / h;
            }
        };
        return solveWithNewtonsMethod(f, dfdx, x0);
    }

    public static Observable<Long> primes() {
        return Observable.defer(new Func0<Observable<Long>>() {
            final Mutable<Long> n = new Mutable<Long>(0L);

            @Override
            public Observable<Long> call() {
                return Observable.just(1).repeat().map(new Func1<Integer, Long>() {
                    @Override
                    public Long call(Integer t) {
                        n.value += 1;
                        return n.value;
                    }
                });
            }
        }).filter(new Func1<Long, Boolean>() {

            @Override
            public Boolean call(Long n) {
                if (n < 2) {
                    return false;
                }
                for (int i = 2; i <= Math.floor(Math.sqrt(n)); i++) {
                    if (n % i == 0) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    private static final class Mutable<T> {
        T value;

        Mutable(T value) {
            this.value = value;
        }

    }

}
