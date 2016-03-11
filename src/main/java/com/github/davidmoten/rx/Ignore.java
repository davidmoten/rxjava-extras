package com.github.davidmoten.rx;

import com.github.davidmoten.rx.Checked.A0;
import com.github.davidmoten.rx.Checked.A1;

import rx.functions.Action0;
import rx.functions.Action1;

public final class Ignore {
    
    private Ignore() {
        //prevent instantiation
    }

    public static Action0 a0(final A0 a) {
        return new Action0() {
            @Override
            public void call() {
                try {
                    a.call();
                } catch (Exception e) {
                    // ignore
                }
            }
        };
    }

    public static <T> Action1<T> a1(final A1<T> a) {
        return new Action1<T>() {
            @Override
            public void call(T t) {
                try {
                    a.call(t);
                } catch (Exception e) {
                    // ignore
                }
            }
        };
    }

}
