package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicLong;

import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;
import rx.internal.operators.BackpressureUtils;

public final class OnSubscribeRepeating<T> implements OnSubscribe<T> {

    private final T value;

    public OnSubscribeRepeating(T value) {
        this.value = value;
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        RepeatingProducer<T> producer = new RepeatingProducer<T>(subscriber, value);
        subscriber.setProducer(producer);
    }

    @SuppressWarnings("serial")
    private static final class RepeatingProducer<T> extends AtomicLong implements Producer {

        private final Subscriber<? super T> subscriber;
        private final T v;

        public RepeatingProducer(Subscriber<? super T> subscriber, T v) {
            this.subscriber = subscriber;
            this.v = v;
        }

        @Override
        public void request(long n) {
            if (n < 0) {
                throw new IllegalArgumentException("reuest must be >=0");
            } else if (n == 0) {
                return;
            } else if (BackpressureUtils.getAndAddRequest(this, n) == 0) {
                long requested = n;
                long emitted = 0;
                do {
                    emitted = requested;
                    while (requested-- > 0 && !subscriber.isUnsubscribed()) {
                        subscriber.onNext(v);
                    }
                } while ((requested = this.addAndGet(-emitted)) > 0);
            }
        }

    }

}
