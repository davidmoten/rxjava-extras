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

public final class TransformerDelayUnsubscribeForRefCount<T> implements Transformer<T, T> {

    private final long delayMs;
    private final Scheduler scheduler;

    public TransformerDelayUnsubscribeForRefCount(long delayMs, Scheduler scheduler) {
        this.delayMs = delayMs;
        this.scheduler = scheduler;
    }

    @Override
    public Observable<T> call(final Observable<T> o) {
        final AtomicInteger count = new AtomicInteger();
        final AtomicReference<Subscriber<T>> extra = new AtomicReference<Subscriber<T>>();
        final AtomicReference<Worker> worker = new AtomicReference<Worker>();
        final Object lock = new Object();
        return o //
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        if (count.incrementAndGet() == 1) {
                            System.out.println(scheduler.now() + ": first");
                            Subscriber<T> sub = doNothing();
                            Worker w;
                            synchronized (lock) {
                                extra.set(sub);
                                w = worker.get();
                                worker.set(null);
                            }
                            if (w != null) {
                                w.unsubscribe();
                                System.out.println(scheduler.now() + ": cancelled unsub");
                            }
                            o.subscribe(sub);
                        } else {
                            Worker w;
                            synchronized (lock) {
                                w = worker.get();
                                worker.set(null);
                            }
                            if (w != null) {
                                w.unsubscribe();
                            }
                        }
                    }
                }) //
                .lift(new OperatorAddToSubscription<T>(new Action0() {

                    @Override
                    public void call() {
                        if (count.decrementAndGet() == 0) {
                            System.out.println(scheduler.now()+ ": to 0");
                            final Worker newW;
                            Worker w;
                            synchronized (lock) {
                                w = worker.get();
                                newW = scheduler.createWorker();
                                worker.set(newW);
                            }
                            if (w != null) {
                                w.unsubscribe();
                                System.out.println(scheduler.now() + ": cancelled unsub while unsub");
                            }
                            newW.schedule(new Action0() {
                                @Override
                                public void call() {
                                    System.out.println(scheduler.now() + ": unsub action");
                                    Subscriber<T> sub;
                                    synchronized (lock) {
                                        sub = extra.get();
                                        extra.set(null);
                                    }
                                    sub.unsubscribe();
                                    System.out.println(scheduler.now() + ": unsubscribed extra");
                                    newW.unsubscribe();
                                    worker.compareAndSet(newW, null);
                                }
                            }, delayMs, TimeUnit.MILLISECONDS);
                            System.out.println(scheduler.now() + ": scheduled unsub");
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
            child.add(parent);
            return parent;
        }

    }

}
