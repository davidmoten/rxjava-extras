package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

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
        assertEquals(Math.pow(9, 0.33333333333333), x, 0.000001);
    }

    @Test
    public void testScan() {
        Observable.range(1, Integer.MAX_VALUE)
                //
                .scan(1, new Func2<Integer, Integer, Integer>() {
                    @Override
                    public Integer call(Integer x, Integer y) {
                        return x;
                    }
                })
                //
                .elementAt(0).toBlocking().single();
    }

    @Test
    public void testPrimes() {
        List<Long> list = Maths.primes().take(10).toList().toBlocking().single();
        assertEquals(Arrays.asList(2L, 3L, 5L, 7L, 11L, 13L, 17L, 19L, 23L, 29L), list);
    }

}
