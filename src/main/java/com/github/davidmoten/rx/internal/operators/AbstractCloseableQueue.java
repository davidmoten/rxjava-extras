package com.github.davidmoten.rx.internal.operators;

import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;

public abstract class AbstractCloseableQueue<T> implements CloseableQueue<T> {

	private final Queue<T> q;
	
	AbstractCloseableQueue(Queue<T> q) {
		this.q = q;
	}

	public boolean add(T e) {
		return q.add(e);
	}

	public boolean offer(T e) {
		return q.offer(e);
	}

	public int size() {
		return q.size();
	}

	public boolean isEmpty() {
		return q.isEmpty();
	}

	public boolean contains(Object o) {
		return q.contains(o);
	}

	public T remove() {
		return q.remove();
	}

	public T poll() {
		return q.poll();
	}

	public T element() {
		return q.element();
	}

	public Iterator<T> iterator() {
		return q.iterator();
	}

	public T peek() {
		return q.peek();
	}

	public Object[] toArray() {
		return q.toArray();
	}

	@SuppressWarnings("hiding")
	public <T> T[] toArray(T[] a) {
		return q.toArray(a);
	}

	public boolean remove(Object o) {
		return q.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return q.containsAll(c);
	}

	public boolean addAll(Collection<? extends T> c) {
		return q.addAll(c);
	}

	public boolean removeAll(Collection<?> c) {
		return q.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return q.retainAll(c);
	}

	public void clear() {
		q.clear();
	}

	public boolean equals(Object o) {
		return q.equals(o);
	}

	public int hashCode() {
		return q.hashCode();
	}

}
