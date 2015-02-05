package com.github.davidmoten.rx;

import rx.Observable;
import rx.functions.Func1;

/**
 * A convenient sorthand way of specifying a Func1 used in a flatMap for
 * instance.
 *
 * @param <T>
 *            item type
 */
public interface Flattener<T> extends Func1<T, Observable<T>> {

}
