package com.github.davidmoten.rx;

import static com.github.davidmoten.util.Optional.absent;
import static com.github.davidmoten.util.Optional.of;
import static rx.Observable.just;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.util.Optional;
import com.github.davidmoten.util.Preconditions;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * Provides builder for the {@link Func1} parameter of
 * {@link Observable#retryWhen(Func1)}. For example:
 * 
 * <pre>
 * o.retryWhen(RetryWhen.maxRetries(4).delay(10, TimeUnit.SECONDS).action(log).build());
 * </pre>
 * 
 * <p>
 * or
 * </p>
 * 
 * <pre>
 * o.retryWhen(RetryWhen.exponentialBackoff(100, TimeUnit.MILLISECONDS).maxRetries(10).build());
 * </pre>
 */
public final class RetryWhen {

	private static final long NO_MORE_DELAYS = -1;

	private static Func1<Observable<? extends Throwable>, Observable<?>> notificationHandler(
			final Observable<Long> delays, final Scheduler scheduler, final Action1<? super ErrorAndDuration> action,
			final List<Class<? extends Throwable>> retryExceptions,
			final List<Class<? extends Throwable>> failExceptions,
			final Func1<? super Throwable, Boolean> exceptionPredicate) {

		final Func1<ErrorAndDuration, Observable<ErrorAndDuration>> checkExceptions = createExceptionChecker(
				retryExceptions, failExceptions, exceptionPredicate);

		return createNotificationHandler(delays, scheduler, action, checkExceptions);
	}

	private static Func1<Observable<? extends Throwable>, Observable<?>> createNotificationHandler(
			final Observable<Long> delays, final Scheduler scheduler, final Action1<? super ErrorAndDuration> action,
			final Func1<ErrorAndDuration, Observable<ErrorAndDuration>> checkExceptions) {
		return new Func1<Observable<? extends Throwable>, Observable<?>>() {

			@Override
			public Observable<ErrorAndDuration> call(Observable<? extends Throwable> errors) {
				return errors
						// zip with delays, use -1 to signal completion
						.zipWith(delays.concatWith(just(NO_MORE_DELAYS)), TO_ERROR_AND_DURATION)
						// check retry and non-retry exceptions
						.flatMap(checkExceptions)
						// perform user action (for example log that a
						// delay is happening)
						.doOnNext(callActionExceptForLast(action))
						// delay the time in ErrorAndDuration
						.flatMap(delay(scheduler));
			}
		};
	}

	private static Action1<ErrorAndDuration> callActionExceptForLast(final Action1<? super ErrorAndDuration> action) {
		return new Action1<ErrorAndDuration>() {

			@Override
			public void call(ErrorAndDuration e) {
				if (e.durationMs() != NO_MORE_DELAYS)
					action.call(e);
			}

		};
	}

	// TODO unit test
	private static Func1<ErrorAndDuration, Observable<ErrorAndDuration>> createExceptionChecker(
			final List<Class<? extends Throwable>> retryExceptions,
			final List<Class<? extends Throwable>> failExceptions,
			final Func1<? super Throwable, Boolean> exceptionPredicate) {
		return new Func1<ErrorAndDuration, Observable<ErrorAndDuration>>() {

			@Override
			public Observable<ErrorAndDuration> call(ErrorAndDuration e) {
				if (!exceptionPredicate.call(e.throwable()))
					return Observable.error(e.throwable());
				for (Class<? extends Throwable> cls : failExceptions) {
					if (e.throwable().getClass().isAssignableFrom(cls))
						return Observable.error(e.throwable());
				}
				if (retryExceptions.size() > 0) {
					for (Class<? extends Throwable> cls : retryExceptions) {
						if (e.throwable().getClass().isAssignableFrom(cls))
							return just(e);
					}
					return Observable.error(e.throwable());
				} else {
					return just(e);
				}
			}
		};
	}

	private static Func2<Throwable, Long, ErrorAndDuration> TO_ERROR_AND_DURATION = new Func2<Throwable, Long, ErrorAndDuration>() {
		@Override
		public ErrorAndDuration call(Throwable throwable, Long durationMs) {
			return new ErrorAndDuration(throwable, durationMs);
		}
	};

	private static Func1<ErrorAndDuration, Observable<ErrorAndDuration>> delay(final Scheduler scheduler) {
		return new Func1<ErrorAndDuration, Observable<ErrorAndDuration>>() {
			@Override
			public Observable<ErrorAndDuration> call(ErrorAndDuration e) {
				if (e.durationMs() == NO_MORE_DELAYS)
					return Observable.error(e.throwable());
				else
					return Observable.timer(e.durationMs(), TimeUnit.MILLISECONDS, scheduler)
							.map(Functions.constant(e));
			}
		};
	}

	// Builder factory methods

	public static Builder retryWhenInstanceOf(Class<? extends Throwable>... classes) {
		return new Builder().retryWhenInstanceOf(classes);
	}

	public static Builder failWhenInstanceOf(Class<? extends Throwable>... classes) {
		return new Builder().failWhenInstanceOf(classes);
	}

	public static Builder retryIf(Func1<Throwable, Boolean> predicate) {
		return new Builder().retryIf(predicate);
	}

	public static Builder delays(Observable<Long> delays, TimeUnit unit) {
		return new Builder().delays(delays, unit);
	}

	public static Builder delaysInt(Observable<Integer> delays, TimeUnit unit) {
		return new Builder().delaysInt(delays, unit);
	}

	public static Builder delay(long delay, final TimeUnit unit) {
		return new Builder().delay(delay, unit);
	}

	public static Builder maxRetries(int maxRetries) {
		return new Builder().maxRetries(maxRetries);
	}

	public static Builder scheduler(Scheduler scheduler) {
		return new Builder().scheduler(scheduler);
	}

	public Builder action(Action1<? super ErrorAndDuration> action) {
		return new Builder().action(action);
	}

	public static Builder exponentialBackoff(final long firstDelay, final TimeUnit unit, final double factor) {
		return new Builder().exponentialBackoff(firstDelay, unit, factor);
	}

	public static Builder exponentialBackoff(long firstDelay, TimeUnit unit) {
		return new Builder().exponentialBackoff(firstDelay, unit);
	}

	public static final class Builder {

		private final List<Class<? extends Throwable>> retryExceptions = new ArrayList<Class<? extends Throwable>>();
		private final List<Class<? extends Throwable>> failExceptions = new ArrayList<Class<? extends Throwable>>();
		private Func1<? super Throwable, Boolean> exceptionPredicate = Functions.alwaysTrue();

		private Observable<Long> delays = Observable.just(0L).repeat();
		private Optional<Integer> maxRetries = absent();
		private Optional<Scheduler> scheduler = of(Schedulers.computation());
		private Action1<? super ErrorAndDuration> action = Actions.doNothing1();

		private Builder() {
			// must use static factory method to instantiate
		}

		public Builder retryWhenInstanceOf(Class<? extends Throwable>... classes) {
			retryExceptions.addAll(Arrays.asList(classes));
			return this;
		}

		public Builder failWhenInstanceOf(Class<? extends Throwable>... classes) {
			failExceptions.addAll(Arrays.asList(classes));
			return this;
		}

		public Builder retryIf(Func1<Throwable, Boolean> predicate) {
			this.exceptionPredicate = predicate;
			return this;
		}

		public Builder delays(Observable<Long> delays, TimeUnit unit) {
			this.delays = delays.map(toMillis(unit));
			return this;
		}
		
		private static class ToLongHolder {
			static final Func1<Integer, Long> INSTANCE = new Func1<Integer, Long>() {
				@Override
				public Long call(Integer n) {
					if (n == null) {
						return null;
					} else {
						return n.longValue();
					}
				}
			};
		}
		
		public Builder delaysInt(Observable<Integer> delays, TimeUnit unit) {
			return delays(delays.map(ToLongHolder.INSTANCE), unit);
		}
		
		public Builder delay(Long delay, final TimeUnit unit) {
			this.delays = Observable.just(delay).map(toMillis(unit)).repeat();
			return this;
		}

		private static Func1<Long, Long> toMillis(final TimeUnit unit) {
			return new Func1<Long, Long>() {

				@Override
				public Long call(Long t) {
					return unit.toMillis(t);
				}
			};
		}

		public Builder maxRetries(int maxRetries) {
			this.maxRetries = of(maxRetries);
			return this;
		}

		public Builder scheduler(Scheduler scheduler) {
			this.scheduler = of(scheduler);
			return this;
		}

		public Builder action(Action1<? super ErrorAndDuration> action) {
			this.action = action;
			return this;
		}

		public Builder exponentialBackoff(final long firstDelay, final TimeUnit unit, final double factor) {
			delays = Observable.range(1, Integer.MAX_VALUE)
					// make exponential
					.map(new Func1<Integer, Long>() {
						@Override
						public Long call(Integer n) {
							return Math.round(Math.pow(factor, n - 1) * unit.toMillis(firstDelay));
						}
					});
			return this;
		}

		public Builder exponentialBackoff(long firstDelay, TimeUnit unit) {
			return exponentialBackoff(firstDelay, unit, 2);
		}

		public Func1<Observable<? extends Throwable>, Observable<?>> build() {
			Preconditions.checkNotNull(delays);
			if (maxRetries.isPresent()) {
				delays = delays.take(maxRetries.get());
			}
			return notificationHandler(delays, scheduler.get(), action, retryExceptions, failExceptions,
					exceptionPredicate);
		}

	}

	public static final class ErrorAndDuration {

		private final Throwable throwable;
		private final long durationMs;

		public ErrorAndDuration(Throwable throwable, long durationMs) {
			this.throwable = throwable;
			this.durationMs = durationMs;
		}

		public Throwable throwable() {
			return throwable;
		}

		public long durationMs() {
			return durationMs;
		}

	}
}
