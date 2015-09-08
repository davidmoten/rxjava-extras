package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static rx.Observable.range;

import java.util.Arrays;
import java.util.List;
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

    @Test
    public void testRequestOverflow() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(Long.MAX_VALUE - 2);
        Observable.range(1, 10)
                //
                .doOnNext(new Action1<Integer>() {

                    @Override
                    public void call(Integer n) {
                        ts.requestMore(5);
                    }
                }) // buffer
                .compose(Transformers.<Integer> bufferEmissions())
                //
                .subscribe(ts);
        ts.assertCompleted();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), ts.getOnNextEvents());
    }

    @Test
    public void testWithMaxValueRequests() {
        List<Integer> list = range(1, 3).compose(Transformers.<Integer> bufferEmissions()).toList()
                .toBlocking().single();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testUnsubscribeAfterOne() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {

            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                unsubscribe();
            }

        };
        range(1, 3).compose(Transformers.<Integer> bufferEmissions()).subscribe(ts);
        ts.assertUnsubscribed();
        ts.assertValues(1);
    }

    @Test
    public void testError() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        range(1, 3).concatWith(Observable.<Integer> error(new RuntimeException()))
                .compose(Transformers.<Integer> bufferEmissions()).subscribe(ts);
        ts.assertUnsubscribed();
        ts.assertValues(1, 2, 3);
        ts.assertError(RuntimeException.class);
    }

    @Test
    public void testBackpressureOneByOne() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
        Observable.just(1, 2, 3).compose(Transformers.<Integer> bufferEmissions()).subscribe(ts);
        ts.assertNoValues();
        ts.requestMore(1);
        ts.assertValues(1);
        ts.requestMore(1);
        ts.assertValues(1, 2);
    }
}
