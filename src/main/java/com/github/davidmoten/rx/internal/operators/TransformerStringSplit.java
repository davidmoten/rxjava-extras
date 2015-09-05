package com.github.davidmoten.rx.internal.operators;

import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Action2;
import rx.functions.Func3;

public final class TransformerStringSplit {

    public static <T> Transformer<String, String> split(final String pattern) {
        LeftOver initialState = new LeftOver(null);
        Func3<LeftOver, String, Subscriber<String>, LeftOver> transition = new Func3<LeftOver, String, Subscriber<String>, LeftOver>() {

            @Override
            public LeftOver call(LeftOver leftOver, String s, Subscriber<String> observer) {
                String[] parts = s.split(pattern, -1);
                // prepend leftover to the first part
                if (leftOver.value != null)
                    parts[0] = leftOver.value + parts[0];

                // can emit all parts except the last part because it hasn't
                // been terminated by the pattern/end-of-stream yet
                for (int i = 0; i < parts.length - 1; i++) {
                    if (observer.isUnsubscribed()) {
                        // won't be used so can return null
                        return null;
                    }
                    observer.onNext(parts[i]);
                }

                // we have to assign the last part as leftOver because we
                // don't know if it has been terminated yet
                return new LeftOver(parts[parts.length - 1]);
            }
        };

        Action2<LeftOver, Subscriber<String>> completionAction = new Action2<LeftOver, Subscriber<String>>() {

            @Override
            public void call(LeftOver leftOver, Subscriber<String> observer) {
                if (leftOver.value != null && !observer.isUnsubscribed())
                    observer.onNext(leftOver.value);
                if (!observer.isUnsubscribed())
                    observer.onCompleted();
            }
        };
        return com.github.davidmoten.rx.Transformers.stateMachine(initialState, transition,
                completionAction);
    }

    private static class LeftOver {
        final String value;

        private LeftOver(String value) {
            this.value = value;
        }
    }

}
