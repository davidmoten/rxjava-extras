package com.github.davidmoten.rx.operators;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

import com.github.davidmoten.rx.Transformers;

public class OperatorBufferEmissionsTest {
    @Test
    public void testSyncDrainerDeliversRequestedFromBackpressureEnabledSource() {
        final AtomicInteger count = new AtomicInteger(0);
        createSource()
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

    private static Observable<Integer> createSource() {
        return Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 0; i < 1000; i++)
                    subscriber.onNext(i);
                subscriber.onCompleted();
            }

        });
    }

    @Test
    public void testUpstreamRequests() {
        final AtomicLong requests = new AtomicLong();
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(100);
        Observable.range(1, 1000)
        //
                .doOnRequest(new Action1<Long>() {

                    @Override
                    public void call(Long n) {
                        requests.addAndGet(n);
                    }
                }) // buffer
                .compose(Transformers.<Integer> bufferEmissions())
                //
                .subscribe(ts);
        assertEquals(100, requests.get());
    }

}
