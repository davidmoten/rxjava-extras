package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.Subscribers;
import rx.subscriptions.Subscriptions;

public class TransformerDelayUnsubscribe<T> implements Transformer<T, T> {

    private final long delayMs;
    private final Scheduler scheduler;

    public TransformerDelayUnsubscribe(long delayMs, Scheduler scheduler) {
        this.delayMs = delayMs;
        this.scheduler = scheduler;
    }

    @Override
    public Observable<T> call(final Observable<T> o) {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Subscriber<T>> extra = new AtomicReference<Subscriber<T>>();
        final AtomicReference<Worker> worker = new AtomicReference<Worker>();
        return o.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                if (count.incrementAndGet() == 1) {
                    Subscriber<T> sub = doNothing();
                    extra.set(sub);
                    o.subscribe(sub);
                }
            }
        }) //
                .lift(new OperatorAddToSubscription<T>(new Action0() {

                    @Override
                    public void call() {
                        if (count.decrementAndGet() == 0) {
                            Worker w = worker.get();
                            if (w != null) {
                                w.unsubscribe();
                            }
                            w = scheduler.createWorker();
                            worker.set(w);
                            w.schedule(new Action0() {
                                @Override
                                public void call() {
                                    extra.get().unsubscribe();
                                }
                            }, delayMs, TimeUnit.MILLISECONDS);
                        }
                    }
                }));
    }

    private static <T> Subscriber<T> doNothing() {
        return new Subscriber<T>() {

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(T t) {
            }
        };
    }

    private static final class OperatorAddToSubscription<T> implements Operator<T, T> {

        private final Action0 action;

        OperatorAddToSubscription(Action0 action) {
            this.action = action;
        }

        @Override
        public Subscriber<? super T> call(Subscriber<? super T> child) {
            Subscriber<T> parent = Subscribers.wrap(child);
            child.add(Subscriptions.create(action));
            return parent;
        }

    }

}
