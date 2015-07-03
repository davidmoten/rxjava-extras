package com.github.davidmoten.rx.util;

import rx.Observer;
import rx.Producer;

public interface Drainer<T> extends Observer<T>, Producer {

    /**
     * Returns the current best estimate of
     * 
     * <pre>
     * totalRequested - currentExpected - numQueuedEmissions - numEmitted
     * </pre>
     * <p>
     * This value can then be used added to a request from downstream to
     * estimate the number required from upstream.
     * 
     * @return
     */
    long total();
}
