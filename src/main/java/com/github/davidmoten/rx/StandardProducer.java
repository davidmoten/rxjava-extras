package com.github.davidmoten.rx;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import rx.Producer;
import rx.Subscriber;

public class StandardProducer<T> implements Producer {

    private final AtomicLong requested = new AtomicLong(0);
    private final Subscriber<? super T> subscriber;
    private final Emitter emitter;

    public StandardProducer(Subscriber<? super T> subscriber, EmitterFactory<T> emitterFactory) {
        this.subscriber = subscriber;
        this.emitter = emitterFactory.create(subscriber);
    }

    @Override
    public void request(long n) {
        try {
            if (requested.get() == Long.MAX_VALUE)
                // already started with fast path
                return;
            else if (n == Long.MAX_VALUE) {
                // fast path
                requestAll();
            } else
                requestSome(n);
        } catch (RuntimeException e) {
            subscriber.onError(e);
        } catch (IOException e) {
            subscriber.onError(e);
        }
    }

    private void requestAll() {
        requested.set(Long.MAX_VALUE);
        emitter.emitAll();
    }

    private void requestSome(long n) throws IOException {
        // back pressure path
        // this algorithm copied roughly from
        // rxjava/OnSubscribeFromIterable.java
        long previousCount = requested.getAndAdd(n);
        if (previousCount == 0) {
            while (true) {
                long r = requested.get();
                LongWrapper numToEmit = new LongWrapper(r);
                emitter.emitSome(numToEmit);
                if (subscriber.isUnsubscribed())
                    return;
                else if (requested.addAndGet(-r) == 0)
                    return;
            }
        }
    }

}