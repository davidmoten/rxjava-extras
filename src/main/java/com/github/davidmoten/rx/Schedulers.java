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
        return new SchedulerWithId(scheduler, describeCallSite());
    }

    private static String describeCallSite() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StackTraceElement e = elements[3];
        return e.getClassName() + ":" + e.getMethodName() + ":" + e.getLineNumber();
    }

    private static void doIt() {
        System.out.println(describeCallSite());
    }

    public static void main(String[] args) {
        doIt();
    }
}
