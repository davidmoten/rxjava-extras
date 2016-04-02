package com.github.davidmoten.rx.internal.operators;

import java.util.Queue;

public interface CloseableQueue<T> extends Queue<T> {

	void close();
	
}
