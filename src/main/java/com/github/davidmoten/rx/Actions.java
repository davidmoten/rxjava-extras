package com.github.davidmoten.rx;

import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;

public final class Actions {

    private static final Action0 DO_NOTHING_0 = new Action0() {

        @Override
        public void call() {
            // do nothing
        }
    };

    private static final Action1<Object> DO_NOTHING_1 = new Action1<Object>() {

        @Override
        public void call(Object t) {
            // do nothing
        }
    };

    private static final Action2<Object, Object> DO_NOTHING_2 = new Action2<Object, Object>() {

        @Override
        public void call(Object t, Object t2) {
            // do nothing
        }
    };

    private static final Action3<Object, Object, Object> DO_NOTHING_3 = new Action3<Object, Object, Object>() {

        @Override
        public void call(Object t, Object t2, Object t3) {
            // do nothing
        }
    };

    public static Action0 doNothing() {
        return DO_NOTHING_0;
    }

    @SuppressWarnings("unchecked")
    public static <T> Action1<T> doNothing1() {
        return (Action1<T>) DO_NOTHING_1;
    }

    @SuppressWarnings("unchecked")
    public static <T, R> Action2<T, R> doNothing2() {
        return (Action2<T, R>) DO_NOTHING_2;
    }

    @SuppressWarnings("unchecked")
    public static <T, R, S> Action3<T, R, S> doNothing3() {
        return (Action3<T, R, S>) DO_NOTHING_3;
    }

}
