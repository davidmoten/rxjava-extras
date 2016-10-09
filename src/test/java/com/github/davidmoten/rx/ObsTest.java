package com.github.davidmoten.rx;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.davidmoten.rx.testing.TestingHelper;

import rx.Observable;
import rx.Scheduler.Worker;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

public class ObsTest {

    @Test
    public void testCachedScheduledReset() {
        TestScheduler scheduler = new TestScheduler();
        Worker worker = scheduler.createWorker();
        try {
            final AtomicInteger count = new AtomicInteger(0);
            Observable<Integer> source = Observable.defer(new Func0<Observable<Integer>>() {
                @Override
                public Observable<Integer> call() {
                    return Observable.just(count.incrementAndGet());
                }
            })
                    // cache
                    .compose(Transformers.<Integer> cache(5, TimeUnit.MINUTES, worker));
            assertEquals(1, (int) source.toBlocking().single());
            scheduler.advanceTimeBy(1, TimeUnit.MINUTES);
            assertEquals(1, (int) source.toBlocking().single());
            scheduler.advanceTimeBy(1, TimeUnit.MINUTES);
            assertEquals(1, (int) source.toBlocking().single());
            scheduler.advanceTimeBy(3, TimeUnit.MINUTES);
            assertEquals(2, (int) source.toBlocking().single());
            assertEquals(2, (int) source.toBlocking().single());
        } finally {
            worker.unsubscribe();
        }
    }

    @Test
    public void testCachedResetableScheduledReset() {
        TestScheduler scheduler = new TestScheduler();
        final AtomicInteger count = new AtomicInteger(0);
        Observable<Integer> o = Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.just(count.incrementAndGet());
            }
        });
        CloseableObservableWithReset<Integer> cached = Obs.cache(o, 5, TimeUnit.MINUTES, scheduler);
        assertEquals(1, (int) cached.observable().toBlocking().single());
        scheduler.advanceTimeBy(1, TimeUnit.MINUTES);
        assertEquals(1, (int) cached.observable().toBlocking().single());
        cached.reset();
        scheduler.advanceTimeBy(5, TimeUnit.MINUTES);
        assertEquals(2, (int) cached.observable().toBlocking().single());
        scheduler.advanceTimeBy(1, TimeUnit.MINUTES);
        assertEquals(2, (int) cached.observable().toBlocking().single());
        cached.reset();
        assertEquals(2, (int) cached.observable().toBlocking().single());
        scheduler.advanceTimeBy(5, TimeUnit.MINUTES);
        assertEquals(3, (int) cached.observable().toBlocking().single());
        cached.close();
    }

    @Test
    public void testRepeatingTwo() {
        assertEquals(Arrays.asList(1000, 1000),
                Obs.repeating(1000).take(2).toList().toBlocking().single());
    }

    @Test
    public void testRepeatingZero() {
        Obs.repeating(1000) //
                .to(TestingHelper.<Integer> testWithRequest(0)) //
                .assertNoValues() //
                .assertNotCompleted() //
                .requestMore(1) //
                .assertValue(1000) //
                .assertNotCompleted();
                
    }

    public static void main(String[] args) throws InterruptedException {
        Observable<Date> source = Observable.defer(new Func0<Observable<Date>>() {
            @Override
            public Observable<Date> call() {
                return Observable.just(new Date()).doOnNext(new Action1<Date>() {
                    @Override
                    public void call(Date d) {
                        System.out.println("source emits " + d);
                    }
                });
            }
        });
        final CloseableObservableWithReset<Date> cached = Obs.cache(source, 5, TimeUnit.SECONDS,
                Schedulers.computation());
        Observable<Date> o = cached.observable().doOnSubscribe(new Action0() {
            @Override
            public void call() {
                cached.reset();
            }
        });
        for (int i = 0; i < 30; i++) {
            o.doOnNext(new Action1<Date>() {
                @Override
                public void call(Date t) {
                    System.out.println(t);
                }
            }).subscribe();
            Thread.sleep((i % 5 + 1) * 1000);
        }
        cached.close();
    }

    @Test
    public void testPermutations() {
        assertEquals(24,
                (int) Obs.permutations(Observable.range(0, 4).toList().toBlocking().single())
                        .count().toBlocking().single());
    }

    @Test
    public void testIntervalLong() {
        TestSubscriber<Long> ts = TestSubscriber.create();
        TestScheduler sched = new TestScheduler();
        Obs.intervalLong(1, TimeUnit.SECONDS, sched).subscribe(ts);
        ts.assertNoValues();
        ts.assertNotCompleted();
        sched.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.assertValue(0L);
        sched.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.assertValues(0L, 1L);
        ts.assertNotCompleted();
    }
}
