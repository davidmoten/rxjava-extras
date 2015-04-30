package com.github.davidmoten.rx.operators;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class OnSubscribeRetry<T> implements OnSubscribe<T> {

    private final Observable<T> source;

    public OnSubscribeRetry(Observable<T> source) {
        this.source = source;
    }

    @Override
    public void call(Subscriber<? super T> child) {
        Retry<T> retry = new Retry<T>(source, child);
        retry.init();
    }

    private static class Retry<T> {

        private final Subscriber<? super T> child;
        private final Observable<T> source;
        private final Worker worker;

        public Retry(Observable<T> source, Subscriber<? super T> child) {
            this.source = source;
            this.child = child;
            this.worker = Schedulers.trampoline().createWorker();
        }

        void init() {
            subscribe();
        }

        private void subscribe() {
            Action0 restart = new Action0() {

                @Override
                public void call() {
                    Subscription sub = source.materialize().unsafeSubscribe(
                            new Subscriber<Notification<T>>() {

                                @Override
                                public void onStart() {

                                }

                                @Override
                                public void onCompleted() {
                                    // do nothing
                                }

                                @Override
                                public void onError(Throwable e) {
                                    child.onError(e);
                                }

                                @Override
                                public void onNext(Notification<T> notification) {
                                    if (notification.hasValue())
                                        child.onNext(notification.getValue());
                                    else if (notification.isOnCompleted())
                                        child.onCompleted();
                                    else {
                                        unsubscribe();
                                        // is error we resubscribe
                                        subscribe();
                                    }
                                }
                            });
                }
            };
            worker.schedule(restart);
        }

    }

}
