package com.github.davidmoten.rx;

import java.util.concurrent.Executors;

import org.junit.Test;

import rx.Scheduler;
import rx.Scheduler.Worker;

public class SchedulersTest {
    
    @Test(timeout = 5000)
    public void doesWait() {
        int numThreads = 3;
        Scheduler scheduler = rx.schedulers.Schedulers.from(Executors.newFixedThreadPool(numThreads));
        final Worker worker = scheduler.createWorker();
        Schedulers.blockUntilWorkFinished(scheduler, numThreads);
    }

}
