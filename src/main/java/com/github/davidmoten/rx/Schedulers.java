package com.github.davidmoten.rx;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.rx.internal.SchedulerWithId;

import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.functions.Action0;

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

    public static void blockUntilWorkFinished(Scheduler scheduler, int numThreads, long timeout,
            TimeUnit unit) {
        final CountDownLatch latch = new CountDownLatch(numThreads);
        for (int i = 1; i <= numThreads; i++) {
            final Worker worker = scheduler.createWorker();
            worker.schedule(new Action0() {

                @Override
                public void call() {
                    worker.unsubscribe();
                    latch.countDown();
                }
            });
        }
        try {
            boolean finished = latch.await(timeout, unit);
            if (!finished) {
                throw new RuntimeException("timeout occured waiting for work to finish");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void blockUntilWorkFinished(Scheduler scheduler, int numThreads) {
        blockUntilWorkFinished(scheduler, numThreads, 1, TimeUnit.MINUTES);
    }
}
