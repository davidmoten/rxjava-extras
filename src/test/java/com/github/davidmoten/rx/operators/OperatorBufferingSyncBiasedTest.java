package com.github.davidmoten.rx.operators;

import static rx.Observable.just;

import org.junit.Test;

import rx.Subscriber;

public class OperatorBufferingSyncBiasedTest {

    @Test
    public void test() {
        
        just(1)
        //
        .lift(new OperatorBufferingSyncBiased<Integer>(1))
        //
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Integer t) {
                        System.out.println(t);
                    }
                });
    }
}
