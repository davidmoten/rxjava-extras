package com.github.davidmoten.rx.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.TestSubscriber;

public class OperatorLastTest {

    @Test
    public void testLastOfTenReturnsLast() {
        TestSubscriber<Integer> s = new TestSubscriber<Integer>();
        Observable.range(1, 10).lift(OperatorLast.<Integer> create()).subscribe(s);
        s.assertReceivedOnNext(Arrays.asList(10));
        s.assertNoErrors();
        s.assertTerminalEvent();
        s.assertUnsubscribed();
    }

    @Test
    public void testLastOfEmptyThrowsError() {
        TestSubscriber<Object> s = new TestSubscriber<Object>();
        Observable.empty().lift(OperatorLast.create()).subscribe(s);
        assertEquals(1, s.getOnErrorEvents().size());
        s.assertTerminalEvent();
        s.assertUnsubscribed();
    }

    @Test
    public void testLastOfOneReturnsLast() {
        TestSubscriber<Integer> s = new TestSubscriber<Integer>();
        Observable.just(1).lift(OperatorLast.<Integer> create()).subscribe(s);
        s.assertReceivedOnNext(Arrays.asList(1));
        s.assertNoErrors();
        s.assertTerminalEvent();
        s.assertUnsubscribed();
    }

    @Test
    public void testUnsubscribesFromUpstream() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Observable.just(1).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                unsubscribed.set(true);
            }
        })
        //
                .lift(OperatorLast.<Integer> create()).subscribe();
        assertTrue(unsubscribed.get());
    }

    @Test
    public void testLastWithBackpressure() {
        MySubscriber<Integer> s = new MySubscriber<Integer>(0);
        Observable.just(1).last().subscribe(s);
        assertEquals(0, s.list.size());
        s.requestMore(1);
        assertEquals(1, s.list.size());
    }

    private static class MySubscriber<T> extends Subscriber<T> {

        private long initialRequest;

        MySubscriber(long initialRequest) {
            this.initialRequest = initialRequest;
        }

        final List<T> list = new ArrayList<T>();

        public void requestMore(long n) {
            request(n);
        }

        @Override
        public void onStart() {
            request(initialRequest);
        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(T t) {
            list.add(t);
        }

    }

}
