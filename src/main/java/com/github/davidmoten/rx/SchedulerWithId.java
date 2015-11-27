package com.github.davidmoten.rx;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

public class SchedulerWithId extends Scheduler {

    private final Scheduler scheduler;
    private final String id;
    private final static Pattern pattern = Pattern.compile("\\bschedId=\\[[^\\]]+\\]+\\b");

    public SchedulerWithId(Scheduler scheduler, String id) {
        this.scheduler = scheduler;
        this.id = "[" + id + "]";
    }

    @Override
    public Worker createWorker() {

        final Worker worker = scheduler.createWorker();
        Worker w = new Worker() {

            @Override
            public void unsubscribe() {
                worker.unsubscribe();
            }

            @Override
            public boolean isUnsubscribed() {
                return worker.isUnsubscribed();
            }

            @Override
            public Subscription schedule(final Action0 action) {
                Action0 a = new Action0() {
                    @Override
                    public void call() {
                        setThreadName();
                        action.call();
                    }
                };
                return worker.schedule(a);
            }

            @Override
            public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
                Action0 a = new Action0() {
                    @Override
                    public void call() {
                        setThreadName();
                        action.call();
                    }
                };
                return worker.schedule(a, delayTime, unit);
            }

        };
        return w;

    }

    private void setThreadName() {
        String name = Thread.currentThread().getName();
        String newName = updateNameWithId(name, id);
        Thread.currentThread().setName(newName);
    }

    private static String updateNameWithId(String name, String id) {
        final String newName;
        if (name == null) {
            newName = id;
        } else {
            Matcher matcher = pattern.matcher(name);
            if (matcher.find()) {
                newName = name.replace(matcher.group(), "schedId=" + id);
            } else {
                newName = name + "|schedId=" + id;
            }
        }
        return newName;
    }

}
