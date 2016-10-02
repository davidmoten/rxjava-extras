package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.davidmoten.rx.Actions;
import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.testing.TestingHelper;

import rx.Observable;
import rx.functions.Func1;

public class OnSubscribeMapLastTest {

    @Test
    public void testMapLastRequestAmount() {
        List<Long> list = new ArrayList<Long>();
        Observable.range(1, 10) //
                .doOnRequest(Actions.addTo(list))//
                .compose(Transformers.mapLast(new Func1<Integer, Integer>() {
                    @Override
                    public Integer call(Integer x) {
                        return x + 1;
                    }
                })).to(TestingHelper.<Integer>testWithRequest(1)) //
                .assertNotCompleted() //
                .assertValuesAndClear(1) //
                .requestMore(3) //
                .assertValues(2, 3, 4);
        assertEquals(Arrays.asList(2L, 3L), list);
    }

    @Test
    public void testMapLastHandlesRequestOverflow() {
        List<Long> list = new ArrayList<Long>();
        Observable.range(1, 5) //
                .doOnRequest(Actions.addTo(list))//
                .compose(Transformers.mapLast(new Func1<Integer, Integer>() {
                    @Override
                    public Integer call(Integer x) {
                        return x + 1;
                    }
                })).to(TestingHelper.<Integer>testWithRequest(Long.MAX_VALUE)) //
                .assertCompleted() //
                .assertValues(1, 2, 3, 4, 6);
        assertEquals(Arrays.asList(Long.MAX_VALUE), list);
    }

    @Test(expected = OutOfMemoryError.class)
    public void testMapLastHandlesFatalError() {
        Observable.range(1, 5) //
                .compose(Transformers.mapLast(new Func1<Integer, Integer>() {
                    @Override
                    public Integer call(Integer x) {
                        throw new OutOfMemoryError();
                    }
                })).subscribe();
    }

    @Test
    public void testMapLastHandlesNonFatalError() {
        final RuntimeException e = new RuntimeException();
        Observable.range(1, 5) //
                .compose(Transformers.mapLast(new Func1<Integer, Integer>() {
                    @Override
                    public Integer call(Integer x) {
                        throw e;
                    }
                })).to(TestingHelper.<Integer>test()) //
                .assertError(e);
    }

}
