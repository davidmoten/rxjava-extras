package com.github.davidmoten.rx.operators;

import com.github.davidmoten.rx.Emitter;
import com.github.davidmoten.rx.EmitterFactory;
import com.github.davidmoten.rx.StandardProducer;
import com.github.davidmoten.rx.util.Next;
import com.github.davidmoten.rx.util.Optional;

import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;

public abstract class AbstractOnSubscribe<T> implements OnSubscribe<T>, Next<T> {

    @Override
    public void call(Subscriber<? super T> subscriber) {
        subscriber.setProducer(createProducer(subscriber));
    }

    private Producer createProducer(Subscriber<? super T> subscriber) {
        EmitterFactory<T> factory = standardEmitterFactory(this);
        return new StandardProducer<T>(subscriber, factory);
    }

    private static <T> EmitterFactory<T> standardEmitterFactory(final Next<T> a) {
        return new EmitterFactory<T>() {

            @Override
            public Emitter create(final Subscriber<? super T> subscriber) {
                return createEmitter(a, subscriber);
            }

        };
    }

    private static <T> Emitter createEmitter(final Next<T> a, final Subscriber<? super T> subscriber) {
        return new Emitter() {

            private boolean completed = false;

            @Override
            public void emitAll() {
                while (!subscriber.isUnsubscribed() && !completed()) {
                    emitOne();
                }
            }

            @Override
            public void emitOne() {
                Optional<T> next = a.next();
                if (next.isPresent()) {
                    subscriber.onNext(next.get());
                    if (completed())
                        subscriber.onCompleted();
                } else {
                    subscriber.onCompleted();
                    completed = true;
                }
            }

            @Override
            public boolean completed() {
                return completed;
            }
        };
    }
}
