package com.github.davidmoten.rx;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

public final class Maths {

    private Maths() {
        // prevent instantiation
    }

    public static Observable<Double> solveWithNewtonsMethod(final Func1<Double, Double> f,
            final Func1<Double, Double> dfdx, double x0) {
        return Observable.range(0, Integer.MAX_VALUE).scan(x0,
                new Func2<Double, Integer, Double>() {

                    @Override
                    public Double call(Double xn, Integer n) {
                        return xn - f.call(xn) / dfdx.call(xn);
                    }
                });
    }

    public static Observable<Double> solveWithNewtonsMethod(final Func1<Double, Double> f,
            final double x0, final double h) {
        return Observable.range(0, Integer.MAX_VALUE).scan(x0,
                new Func2<Double, Integer, Double>() {

                    @Override
                    public Double call(Double xn, Integer n) {
                        return xn - f.call(xn) * 2 * h / (f.call(xn + h) - f.call(xn - h));
                    }
                });
    }

}
