package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;

public final class TransformerOnBackpressureBufferPassThroughRequests<T> implements Transformer<T, T> {

	@Override
	public Observable<T> call(final Observable<T> o) {
		return Observable.defer(new Func0<Observable<T>>() {
			@Override
			public Observable<T> call() {
				final OperatorPassThroughRequest<T> op = new OperatorPassThroughRequest<T>();
				return o.lift(op).onBackpressureBuffer().doOnRequest(new Action1<Long>() {
					@Override
					public void call(Long n) {
						op.requestMore(n);
					}
				});
			}
		});
	}

	/**
	 * Only used with an immediate downstream operator that requests
	 * {@code Long.MAX_VALUE} and should only be subscribed to once (use it
	 * within a {@code defer} block}.
	 *
	 * @param <T>
	 *            stream item type
	 */
	private static final class OperatorPassThroughRequest<T> implements Operator<T, T> {

		private volatile ParentSubscriber<T> parent;
		private volatile long reqestedBeforeSubscription = 0;
		private final Object lock = new Object();

		@Override
		public Subscriber<? super T> call(Subscriber<? super T> child) {
			// this method should only be called once for this instance
			// assume child requests MAX_VALUE
			synchronized (lock) {
				parent = new ParentSubscriber<T>(child);
				parent.requestMore(reqestedBeforeSubscription);
			}
			child.add(parent);
			return parent;
		}

		public void requestMore(long n) {
			if (parent != null) {
				parent.requestMore(n);
			} else {
				synchronized(lock) {
					if (parent!=null) {
						parent.requestMore(n);
					} else {
						reqestedBeforeSubscription+=n;
					}
				}
				parent.requestMore(n);
			}
		}

	}

	private static final class ParentSubscriber<T> extends Subscriber<T> {

		private final Subscriber<? super T> child;

		public ParentSubscriber(Subscriber<? super T> child) {
			this.child = child;
		}

		public void requestMore(long n) {
			request(n);
		}

		@Override
		public void onCompleted() {
			child.onCompleted();
		}

		@Override
		public void onError(Throwable e) {
			child.onError(e);
		}

		@Override
		public void onNext(T t) {
			child.onNext(t);
		}

	}

}
