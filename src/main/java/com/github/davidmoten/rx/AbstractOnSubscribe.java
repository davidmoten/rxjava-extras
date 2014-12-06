package com.github.davidmoten.rx;

import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;

public abstract class AbstractOnSubscribe<T> implements OnSubscribe<T> {

    @Override
    public void call(Subscriber<? super T> subscriber) {
        subscriber.setProducer(createProducer(subscriber));
    }

    private Producer createProducer(Subscriber<? super T> subscriber) {

        EmitterFactory<T> factory = new EmitterFactory<T>() {

            @Override
            public Emitter create(final Subscriber<? super T> subscriber) {
                return new Emitter() {

                    @Override
                    public void emitAll() {
                        while (!subscriber.isUnsubscribed() && !completed()) {
                            emitOne();
                        }
                    }

                    @Override
                    public void emitOne() {
                        subscriber.onNext(next());
                        if (completed())
                            subscriber.onCompleted();
                    }

                    @Override
                    public boolean completed() {
                        return completed();
                    }
                };
            }
        };
        return new StandardProducer<T>(subscriber, factory);
    }

    abstract T next();

    abstract T completed();

}
