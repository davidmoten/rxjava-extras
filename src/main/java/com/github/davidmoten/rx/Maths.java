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
        return Obs.repeating(1).scan(x0,
                new Func2<Double, Integer, Double>() {
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
                return (f.call(x + h )- f.call(x-h))/2.0/h;
            }};
        return solveWithNewtonsMethod(f, dfdx, x0);
    }

}
