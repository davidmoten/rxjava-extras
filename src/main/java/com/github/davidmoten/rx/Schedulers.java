package com.github.davidmoten.rx;

import rx.Scheduler;

public final class Schedulers {

    public static Scheduler computation(String id) {
        return new SchedulerWithId(rx.schedulers.Schedulers.computation(), id);
    }

    public static Scheduler computation() {
        return withId(rx.schedulers.Schedulers.computation());
    }

    private static Scheduler withId(Scheduler scheduler) {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StackTraceElement e = elements[1];
        return new SchedulerWithId(Schedulers.computation(),
                e.getClassName() + ":" + e.getMethodName() + ":" + e.getLineNumber());
    }
}
