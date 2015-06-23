package com.github.davidmoten.rx.util;

import rx.Observer;
import rx.Producer;

public interface Drainer<T> extends Observer<T>, Producer {

}
