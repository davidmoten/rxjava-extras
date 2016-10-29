package com.github.davidmoten.rx.internal.operators;

import org.junit.Test;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.testing.TestingHelper;

import rx.Observable;

public final class ObservableReverseTest {

    @Test
    public void testEmpty() {
        Observable.empty() //
                .compose(Transformers.reverse()) //
                .to(TestingHelper.test()) //
                .assertNoValues() //
                .assertCompleted();
    }
    
    @Test
    public void testOne() {
        Observable.just(1) //
                .compose(Transformers.reverse()) //
                .to(TestingHelper.test()) //
                .assertValue(1) //
                .assertCompleted();
    }
    
    @Test
    public void testMany() {
        Observable.just(1,2,3,4,5) //
                .compose(Transformers.reverse()) //
                .to(TestingHelper.test()) //
                .assertValues(5,4,3,2,1) //
                .assertCompleted();
    }
    
}
