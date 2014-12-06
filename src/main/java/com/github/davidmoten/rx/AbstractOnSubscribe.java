package com.github.davidmoten.rx;

import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;

public abstract class AbstractOnSubscribe<T> implements OnSubscribe<T> {

    protected boolean completed = false;

    abstract Optional<T> next();

    public boolean completed() {
        return completed;
    }

    public void onCompleted() {
        this.completed = false;
    }

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
                        Optional<T> next = next();
                        if (next.isPresent()) {
                            subscriber.onNext(next.get());
                            if (completed())
                                subscriber.onCompleted();
                        } else {
                            subscriber.onCompleted();
                            onCompleted();
                        }
                    }

                    @Override
                    public boolean completed() {
                        return completed;
                    }
                };
            }
        };
        return new StandardProducer<T>(subscriber, factory);
    }

}
