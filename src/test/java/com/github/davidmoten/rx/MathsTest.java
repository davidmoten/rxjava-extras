package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rx.functions.Func1;

public class MathsTest {

    @Test
    public void testNewtonsSolver() {
        Func1<Double, Double> f = new Func1<Double, Double>() {

            @Override
            public Double call(Double x) {
                return x * x * x - 9;
            }
        };
        double x = Maths.solveWithNewtonsMethod(f, 1, 0.01).elementAt(100).toBlocking().single();
        assertEquals(Math.pow(9,  0.33333333333333), x, 0.000001);
    }

}
