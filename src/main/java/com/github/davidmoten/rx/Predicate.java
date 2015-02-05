package com.github.davidmoten.rx;

import rx.functions.Func1;

/**
 * A convenient sorthand way of specifying a Func1 used in a filter for
 * instance.
 * 
 * @param <T>
 *            item type
 */
public interface Predicate<T> extends Func1<T, Boolean> {

}
