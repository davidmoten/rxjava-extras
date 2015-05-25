package com.github.davidmoten.util;

import rx.Observer;
import rx.Producer;

public interface Drainer<T> extends Observer<T>, Producer {

}
