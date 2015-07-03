package com.github.davidmoten.rx.util;

import rx.Observer;
import rx.Producer;

public interface Drainer<T> extends Observer<T>, Producer {

    /**
     * Returns the current best estimate of
     * 
     * <pre>
     * currentExpected + numQueuedEmissions + numEmitted - totalRequested -
     * </pre>
     * <p>
     * This value can then be subtracted from a request from downstream to
     * estimate the number required from upstream.
     * 
     * @return
     */
    long surplus();
}
