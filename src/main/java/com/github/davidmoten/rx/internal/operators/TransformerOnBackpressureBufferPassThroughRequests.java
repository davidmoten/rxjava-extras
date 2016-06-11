package com.github.davidmoten.rx.internal.operators;

import java.util.concurrent.atomic.AtomicLong;

import com.github.davidmoten.rx.Actions;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.internal.operators.BackpressureUtils;
import rx.schedulers.Schedulers;

public final class TransformerOnBackpressureBufferPassThroughRequests<T> implements Transformer<T, T> {

	private static final TransformerOnBackpressureBufferPassThroughRequests<Object> instance = new TransformerOnBackpressureBufferPassThroughRequests<Object>();

	@SuppressWarnings("unchecked")
	public static final <T> TransformerOnBackpressureBufferPassThroughRequests<T> instance() {
		return (TransformerOnBackpressureBufferPassThroughRequests<T>) instance;
	}

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
		private volatile long requestedBeforeSubscription = 0;
		private final Object lock = new Object();

		@Override
		public Subscriber<? super T> call(Subscriber<? super T> child) {
			// this method should only be called once for this instance
			// assume child requests MAX_VALUE
			synchronized (lock) {
				parent = new ParentSubscriber<T>(child);
				parent.requestMore(requestedBeforeSubscription);
			}
			child.add(parent);
			return parent;
		}

		public void requestMore(long n) {
			if (parent != null) {
				parent.requestMore(n);
			} else {
				long requestToParent = 0;
				synchronized (lock) {
					if (parent != null) {
						requestToParent = n;
					} else {
						long u = requestedBeforeSubscription + n;
						if (u < 0) {
							requestedBeforeSubscription = Long.MAX_VALUE;
						} else {
							requestedBeforeSubscription += n;
						}
					}
				}
				if (requestToParent > 0) {
					parent.requestMore(requestToParent);
				}
			}
		}

	}

	private static final class ParentSubscriber<T> extends Subscriber<T> {

		private final Subscriber<? super T> child;
		private final AtomicLong emitted = new AtomicLong();
		private final AtomicLong requested = new AtomicLong();

		public ParentSubscriber(Subscriber<? super T> child) {
			this.child = child;
		}

		public void requestMore(long n) {
			long r = requested.get();
			if (r == Long.MAX_VALUE || n == 0) {
				return;
			} else {
				while (true) {
					long u = requested.get();
					long v = u + n;
					if (v < 0) {
						v = Long.MAX_VALUE;
					}
					if (requested.compareAndSet(u, v)) {
						long diff = Math.max(0, r - emitted.get());
						request(Math.min(n, diff));
						break;
					}
				}
			}
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
			emitted.incrementAndGet();
			child.onNext(t);
		}

	}

	public static void main(String[] args) throws InterruptedException {
		Observable.range(1, 10000) //
				.doOnRequest(new Action1<Long>() {
					@Override
					public void call(Long n) {
						System.out.println("requested " + n);
					}
				}).doOnUnsubscribe(new Action0() {
					@Override
					public void call() {
						System.out.println("unsubscribed");
					}
				}) //
				.compose(new TransformerOnBackpressureBufferPassThroughRequests<Integer>()) //
				.take(10).subscribeOn(Schedulers.io()).doOnNext(Actions.println()).count().toBlocking().single();
		Thread.sleep(2000);
	}

}
