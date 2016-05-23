package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.davidmoten.rx.util.Pair;

import rx.Observable;
import rx.Subscription;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

public class TransformersTest {

    @Test
    public void testStatisticsOnEmptyStream() {
        Observable<Integer> nums = Observable.empty();
        Statistics s = nums.compose(Transformers.collectStats()).last().toBlocking().single();
        assertEquals(0, s.count());
        assertEquals(0, s.sum(), 0.0001);
        assertTrue(Double.isNaN(s.mean()));
        assertTrue(Double.isNaN(s.sd()));
    }

    @Test
    public void testStatisticsOnSingleElement() {
        Observable<Integer> nums = Observable.just(1);
        Statistics s = nums.compose(Transformers.collectStats()).last().toBlocking().single();
        assertEquals(1, s.count());
        assertEquals(1, s.sum(), 0.0001);
        assertEquals(1.0, s.mean(), 0.00001);
        assertEquals(0, s.sd(), 0.00001);
    }

    @Test
    public void testStatisticsOnMultipleElements() {
        Observable<Integer> nums = Observable.just(1, 4, 10, 20);
        Statistics s = nums.compose(Transformers.collectStats()).last().toBlocking().single();
        assertEquals(4, s.count());
        assertEquals(35.0, s.sum(), 0.0001);
        assertEquals(8.75, s.mean(), 0.00001);
        assertEquals(7.258615570478987, s.sd(), 0.00001);
    }

    @Test
    public void testStatisticsPairOnEmptyStream() {
        Observable<Integer> nums = Observable.empty();
        boolean isEmpty = nums.compose(Transformers.collectStats(Functions.<Integer> identity()))
                .isEmpty().toBlocking().single();
        assertTrue(isEmpty);
    }

    @Test
    public void testStatisticsPairOnSingleElement() {
        Observable<Integer> nums = Observable.just(1);
        Pair<Integer, Statistics> s = nums
                .compose(Transformers.collectStats(Functions.<Integer> identity())).last()
                .toBlocking().single();
        assertEquals(1, (int) s.a());
        assertEquals(1, s.b().count());
        assertEquals(1, s.b().sum(), 0.0001);
        assertEquals(1.0, s.b().mean(), 0.00001);
        assertEquals(0, s.b().sd(), 0.00001);
    }

    @Test
    public void testStatisticsPairOnMultipleElements() {
        Observable<Integer> nums = Observable.just(1, 4, 10, 20);
        Pair<Integer, Statistics> s = nums
                .compose(Transformers.collectStats(Functions.<Integer> identity())).last()
                .toBlocking().single();
        assertEquals(4, s.b().count());
        assertEquals(35.0, s.b().sum(), 0.0001);
        assertEquals(8.75, s.b().mean(), 0.00001);
        assertEquals(7.258615570478987, s.b().sd(), 0.00001);
    }

    @Test
    public void testSort() {
        Observable<Integer> o = Observable.just(5, 3, 1, 4, 2);
        List<Integer> list = o.compose(Transformers.<Integer> sort()).toList().toBlocking()
                .single();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    public void testSortWithComparator() {
        Observable<Integer> o = Observable.just(5, 3, 1, 4, 2);
        List<Integer> list = o.compose(Transformers.<Integer> sort(new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        })).toList().toBlocking().single();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), list);
    }

    @Test
    public void testDoOnNth() {
        AtomicInteger item = new AtomicInteger();
        Observable.just(1, 2, 3).compose(Transformers.doOnNext(2, Actions.setAtomic(item)))
                .subscribe();
        assertEquals(2, item.get());
    }

    @Test
    public void testDelay() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Long> o = Observable //
                .<Long> never() //
                .doOnSubscribe(Actions.increment0(count)) //
                .share();
        TestSubscriber<Long> ts = TestSubscriber.create();
        Subscription s1 = o.subscribe();
        assertEquals(1, count.get());
        Subscription s2 = o.subscribe();
        assertEquals(1, count.get());
        s1.unsubscribe();
        s2.unsubscribe();
        Subscription s3 = o.subscribe();
        assertEquals(2, count.get());
    }

    @Test
//    @org.junit.Ignore
    public void testDelayFinalUnsubscribeForRefCount() {
        TestScheduler s = new TestScheduler();
        final AtomicInteger count = new AtomicInteger();
        Observable<Long> o = Observable //
                .interval(1, TimeUnit.SECONDS, s) //
                .doOnSubscribe(Actions.increment0(count)) //
                .publish() //
                .refCount() //
                .compose(Transformers.<Long> delayFinalUnsubscribe(2500,
                        TimeUnit.MILLISECONDS, s));
        {
            TestSubscriber<Long> ts1 = TestSubscriber.create();
            o.subscribe(ts1);
            assertEquals(1, count.get());
            s.advanceTimeBy(1, TimeUnit.SECONDS);
            ts1.assertValues(0L);
            ts1.unsubscribe();
        }
        s.advanceTimeBy(1, TimeUnit.SECONDS);
        {
            TestSubscriber<Long> ts2 = TestSubscriber.create();
            o.subscribe(ts2);
            ts2.assertNoValues();
            s.advanceTimeBy(1, TimeUnit.SECONDS);
            ts2.assertValues(2L);
            ts2.unsubscribe();
        }
        assertEquals(1, count.get());
        s.advanceTimeBy(2500, TimeUnit.MILLISECONDS);
        {
            TestSubscriber<Long> ts3 = TestSubscriber.create();
            s.advanceTimeBy(1, TimeUnit.SECONDS);
            o.subscribe(ts3);
            assertEquals(2, count.get());
            s.advanceTimeBy(1, TimeUnit.SECONDS);
            ts3.assertValues(0L);
        }

    }

}
