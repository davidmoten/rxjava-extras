package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.observers.TestSubscriber;

public class OnSubscribeFromIterableTest {

    @Test
    public void testUnsub() throws InterruptedException {

        final List<Integer> list = new ArrayList<Integer>();
        Observable.from(Arrays.asList(1, 2, 3, 4)).subscribe(new TestSubscriber<Integer>() {

            @Override
            public void onCompleted() {
                System.out.println("onComplete");
            }

            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                list.add(t);
                unsubscribe();
            }
        });
        assertEquals(1, list.size());
    }
}
