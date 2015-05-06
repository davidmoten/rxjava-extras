package com.github.davidmoten.rx.operators;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Transformer;
import rx.Producer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subjects.PublishSubject;

import com.github.davidmoten.util.BackpressureUtils;

public class OnSubscribeRetry<T> implements OnSubscribe<T> {

    private final Parameters<T> parameters;

    public OnSubscribeRetry(Observable<T> source, Scheduler scheduler, boolean stopOnComplete,
            boolean stopOnError, Transformer<Notification<T>, Notification<T>> transformer) {
        this.parameters = new Parameters<T>(source, scheduler, stopOnComplete, stopOnError,
                transformer);
    }

    private static class Parameters<T> {
        final Observable<T> source;
        final Scheduler scheduler;
        final boolean stopOnComplete;
        final boolean stopOnError;
        final Transformer<Notification<T>, Notification<T>> transformer;

        Parameters(Observable<T> source, Scheduler scheduler, boolean stopOnComplete,
                boolean stopOnError, Transformer<Notification<T>, Notification<T>> transformer) {
            this.source = source;
            this.scheduler = scheduler;
            this.stopOnComplete = stopOnComplete;
            this.stopOnError = stopOnError;
            this.transformer = transformer;
        }
    }

    @Override
    public void call(Subscriber<? super T> child) {
        child.setProducer(new RetryProducer<T>(parameters, child));
    }

    private static class RetryProducer<T> implements Producer {

        private final AtomicLong expected = new AtomicLong();
        private final AtomicBoolean restart = new AtomicBoolean(true);
        private final Parameters<T> parameters;
        private final Subscriber<? super T> child;
        private final Worker worker;
        private final PublishSubject<Notification<T>> terminals;

        public RetryProducer(Parameters<T> parameters, Subscriber<? super T> child) {
            this.parameters = parameters;
            this.child = child;
            this.worker = parameters.scheduler.createWorker();
            this.terminals = PublishSubject.<Notification<T>> create();
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                BackpressureUtils.getAndAddRequest(expected, n);
                process();
            }
        }

        private synchronized void process() {
            if (restart.compareAndSet(true, false)) {
                restart();
            }

        }

        private void restart() {
            Action0 restart = new Action0() {

                @Override
                public void call() {
                    Subscription sub = parameters.source.materialize()
                    // .transform(parameters.transformer)
                            .<T> dematerialize().unsafeSubscribe(new Subscriber<T>() {

                                @Override
                                public void onStart() {

                                }

                                @Override
                                public void onCompleted() {
                                    // do nothing
                                }

                                @Override
                                public void onError(Throwable e) {
                                }

                                @Override
                                public void onNext(T t) {

                                }
                            });
                }
            };
            worker.schedule(restart);
        }
    }

}
