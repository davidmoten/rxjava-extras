package com.github.davidmoten.rx.operators;

import rx.Notification;
import rx.Observable;
import rx.Observable.Operator;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class OperatorRetry<T> implements Operator<T, T> {

    private final Observable<T> source;

    public OperatorRetry(Observable<T> source) {
        this.source = source;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        ParentSubscriber<T> parent = new ParentSubscriber<T>(source, child);
        parent.init();
        return parent;
    }

    private static class ParentSubscriber<T> extends Subscriber<T> {

        private final Subscriber<? super T> child;
        private final Observable<T> source;
        private final Worker worker;

        public ParentSubscriber(Observable<T> source, Subscriber<? super T> child) {
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
                                    // TODO Auto-generated method stub

                                }

                                @Override
                                public void onError(Throwable e) {
                                    // TODO Auto-generated method stub

                                }

                                @Override
                                public void onNext(Notification<T> notification) {
                                    if (notification.hasValue())
                                        child.onNext(notification.getValue());
                                    else if (notification.isOnCompleted())
                                        child.onCompleted();
                                    else {
                                        // is error we resubscribe
                                        subscribe();
                                    }
                                }
                            });
                }
            };
            worker.schedule(restart);
        }

        @Override
        public void onCompleted() {
            // TODO Auto-generated method stub

        }

        @Override
        public void onError(Throwable e) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onNext(T t) {
            // TODO Auto-generated method stub

        }

    }

}
