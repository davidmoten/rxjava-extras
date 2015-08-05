package com.github.davidmoten.rx.operators;

import org.junit.Test;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.Transformers.ErrorAndWait;

import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

public class TransformerRetryExponentialBackoffTest {

    @Test
    public void test() {
        Exception ex = new IllegalArgumentException("boo");
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Action1<ErrorAndWait> log = new Action1<ErrorAndWait>() {

            @Override
            public void call(ErrorAndWait e) {
                System.out.println("WARN: " + e.throwable().getMessage());
                System.out.println("waiting for " + e.waitMs() + "ms");
            }
        };
        Observable.just(1, 2, 3)
                // force error after 3 emissions
                .concatWith(Observable.<Integer> error(ex))
                // retry with backoff
                .compose(Transformers.<Integer> retryExponentialBackoff(2, 1000, log))
                // go
                .subscribe(ts);

        // check results
        ts.awaitTerminalEvent();
        ts.assertError(ex);
        ts.assertValues(1, 2, 3, 1, 2, 3, 1, 2, 3);
    }

}
