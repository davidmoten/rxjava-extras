package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.Observable;

public class TransformationsTest {

    @Test
    public void testStatistics() {
        Observable<Integer> nums = Observable.just(1, 4, 10, 20);
        Statistics s = nums.compose(Transformations.collectStats()).last().toBlocking().single();
        assertEquals(4, s.count());
        assertEquals(35.0, s.sum(), 0.0001);
        assertEquals(8.75, s.mean(), 0.00001);
        assertEquals(7.258615570478987, s.sd(), 0.00001);
    }

    @Test
    public void testSort() {
        Observable<Integer> o = Observable.just(5, 3, 1, 4, 2);
        List<Integer> list = o.compose(Transformations.<Integer> sort()).toList().toBlocking()
                .single();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

}
