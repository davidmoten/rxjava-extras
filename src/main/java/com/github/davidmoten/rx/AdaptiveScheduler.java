package com.github.davidmoten.rx;

import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

public class AdaptiveScheduler extends Scheduler {

    private final Scheduler scheduler;

    private AdaptiveScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    public static AdaptiveScheduler createAdaptiveScheduler(Scheduler scheduler) {
        return new AdaptiveScheduler(scheduler);
    }

    @Override
    public Worker createWorker() {
        final Worker worker = scheduler.createWorker();
        return new Worker() {

            @Override
            public void unsubscribe() {
                worker.unsubscribe();
            }

            @Override
            public boolean isUnsubscribed() {
                return worker.isUnsubscribed();
            }

            @Override
            public Subscription schedule(Action0 action) {
                return worker.schedule(action);
            }

            @Override
            public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
                return worker.schedule(action, delayTime, unit);
            }
        };
    }
    
}
