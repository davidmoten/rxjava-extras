package com.github.davidmoten.rx.util;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

import com.github.davidmoten.rx.Transformers;

public class DrainerSyncBiasedTest {

    @Test
    public void test() {
        final AtomicInteger count = new AtomicInteger(0);
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 0; i < 1000; i++)
                    subscriber.onNext(i);
                subscriber.onCompleted();
            }

        })
        // buffer
                .compose(Transformers.<Integer> bufferEmissions())
                // subscribe
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(200);
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Integer t) {
                        count.incrementAndGet();
                    }
                });
        assertEquals(200, count.get());
    }
}
