package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.Subscribers;
import rx.subscriptions.Subscriptions;

public final class TransformerDelayFinalUnsubscribe<T> implements Transformer<T, T> {

	private final long delayMs;
	private final Scheduler scheduler;

	public TransformerDelayFinalUnsubscribe(long delayMs, Scheduler scheduler) {
		this.delayMs = delayMs;
		this.scheduler = scheduler;
	}

	@Override
	public Observable<T> call(final Observable<T> o) {
		final AtomicInteger count = new AtomicInteger();
		final AtomicReference<Subscriber<T>> extra = new AtomicReference<Subscriber<T>>();
		final AtomicReference<Worker> worker = new AtomicReference<Worker>();
		final Object lock = new Object();
		return o //
				.doOnSubscribe(new Action0() {
					@Override
					public void call() {
						if (count.incrementAndGet() == 1) {
							final Worker w;
							final Subscriber<T> sub;
							synchronized (lock) {
								if (extra.get() == null) {
									sub = doNothing();
									extra.set(sub);
								} else {
									sub = null;
								}
								w = worker.get();
								worker.set(null);
							}
							if (w != null) {
								w.unsubscribe();
							}
							if (sub != null) {
								o.subscribe(sub);
							}
						} else {
							final Worker w;
							synchronized (lock) {
								w = worker.get();
								worker.set(null);
							}
							if (w != null) {
								w.unsubscribe();
							}
						}
					}
				}) //
				.lift(new OperatorAddToSubscription<T>(new Action0() {

					@Override
					public void call() {
						if (count.decrementAndGet() == 0) {
							final Worker newW;
							final Worker w;
							synchronized (lock) {
								w = worker.get();
								newW = scheduler.createWorker();
								worker.set(newW);
							}
							if (w != null) {
								w.unsubscribe();
							}
							// scheduler unsubscribe
							newW.schedule(new Action0() {
								@Override
								public void call() {
									Subscriber<T> sub;
									synchronized (lock) {
										sub = extra.get();
										extra.set(null);
									}
									sub.unsubscribe();
									newW.unsubscribe();
									worker.compareAndSet(newW, null);
								}
							}, delayMs, TimeUnit.MILLISECONDS);
						}
					}
				}));
	}

	private static <T> Subscriber<T> doNothing() {
		return new Subscriber<T>() {

			@Override
			public void onCompleted() {
			}

			@Override
			public void onError(Throwable e) {
			}

			@Override
			public void onNext(T t) {
			}
		};
	}

	private static final class OperatorAddToSubscription<T> implements Operator<T, T> {

		private final Action0 action;

		OperatorAddToSubscription(Action0 action) {
			this.action = action;
		}

		@Override
		public Subscriber<? super T> call(Subscriber<? super T> child) {
			Subscriber<T> parent = Subscribers.wrap(child);
			child.add(Subscriptions.create(action));
			return parent;
		}

	}

}
