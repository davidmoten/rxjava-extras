package com.github.davidmoten.rx.internal.operators;

import java.io.IOException;

import org.junit.Test;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

public class OperatorOnTerminateResumeTest {

    @Test
    public void mainCompletes() {
        
        Transformer<Integer, Integer> op = new OperatorOnTerminateResume<Integer>(new Func1<Throwable, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Throwable e) {
                return Observable.just(11);
            }
        }, Observable.just(12));
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(1, 10)
        .compose(op)
        .subscribe(ts);
        
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void mainCompletesBackpressure() {
        
        Transformer<Integer, Integer> op = new OperatorOnTerminateResume<Integer>(new Func1<Throwable, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Throwable e) {
                return Observable.just(11);
            }
        }, Observable.just(12));
        
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        Observable.range(1, 10)
        .compose(op)
        .subscribe(ts);
        
        ts.assertNoValues();
        
        ts.requestMore(2);

        ts.assertValues(1, 2);

        ts.requestMore(5);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7);

        ts.requestMore(3);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        ts.requestMore(1);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void mainErrors() {
        
        Transformer<Integer, Integer> op = new OperatorOnTerminateResume<Integer>(new Func1<Throwable, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Throwable e) {
                return Observable.just(11);
            }
        }, Observable.just(12));
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(1, 10).concatWith(Observable.<Integer>error(new IOException()))
        .compose(op)
        .subscribe(ts);
        
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void mainErrorsBackpressure() {
        
        Transformer<Integer, Integer> op = new OperatorOnTerminateResume<Integer>(new Func1<Throwable, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Throwable e) {
                return Observable.just(11);
            }
        }, Observable.just(12));
        
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        Observable.range(1, 10).concatWith(Observable.<Integer>error(new IOException()))
        .compose(op)
        .subscribe(ts);
        
        ts.assertNoValues();
        
        ts.requestMore(2);

        ts.assertValues(1, 2);

        ts.requestMore(5);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7);

        ts.requestMore(3);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        ts.requestMore(1);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
}
