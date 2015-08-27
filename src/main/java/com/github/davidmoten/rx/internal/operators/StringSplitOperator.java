package com.github.davidmoten.rx.internal.operators;

import java.util.regex.Pattern;

import rx.Observable.Operator;
import rx.Subscriber;

/**
 * Splits and joins items in a sequence of strings based on a regex pattern.
 * Supports backpressure when combined with {@link OperatorBufferEmissions}.
 */
public final class StringSplitOperator implements Operator<String, String> {

    /**
     * Pattern to split the strings by.
     */
    private final Pattern pattern;

    /**
     * Constructor.
     * 
     * @param pattern
     */
    public StringSplitOperator(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public Subscriber<? super String> call(final Subscriber<? super String> child) {
        return new Subscriber<String>(child) {

            // the bit left over from that last string that hasn't been
            // terminated yet
            private String leftOver = null;

            @Override
            public void onCompleted() {
                // fast path
                if (leftOver != null)
                    child.onNext(leftOver);
                if (!isUnsubscribed()) {
                    child.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                // fast path
                child.onError(e);
            }

            @Override
            public void onNext(String s) {
                String[] parts = pattern.split(s, -1);
                // prepend leftover to the first part
                if (leftOver != null)
                    parts[0] = leftOver + parts[0];

                // can emit all parts except the last part because it hasn't
                // been terminated by the pattern/end-of-stream yet
                for (int i = 0; i < parts.length - 1; i++)
                    child.onNext(parts[i]);

                if (parts.length == 1)
                    // ensure that the stream does not stall
                    request(1);
                // we have to assign the last part as leftOver because we
                // don't know if it has been terminated yet
                leftOver = parts[parts.length - 1];
            }
        };
    }

}
