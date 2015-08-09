package com.github.davidmoten.rx;

import static com.github.davidmoten.util.Optional.absent;
import static com.github.davidmoten.util.Optional.of;
import static rx.Observable.just;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.util.ErrorAndDuration;
import com.github.davidmoten.util.Optional;
import com.google.common.base.Preconditions;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class RetryWhen {

    private static final long NO_MORE_WAITS = -1;

    static Func1<Observable<? extends Throwable>, Observable<?>> notificationHandler(
            final Observable<Long> waits, final Scheduler scheduler,
            final Action1<? super ErrorAndDuration> action,
            final List<Class<? extends Throwable>> retryExceptions,
            final List<Class<? extends Throwable>> failExceptions,
            final Func1<? super Throwable, Boolean> exceptionPredicate) {
        final Func1<ErrorAndDuration, Observable<ErrorAndDuration>> checkExceptions = new Func1<ErrorAndDuration, Observable<ErrorAndDuration>>() {

            @Override
            public Observable<ErrorAndDuration> call(ErrorAndDuration e) {
                if (!exceptionPredicate.call(e.throwable()))
                    return Observable.error(e.throwable());
                for (Class<? extends Throwable> cls : failExceptions) {
                    if (e.throwable().getClass().isAssignableFrom(cls))
                        return Observable.<ErrorAndDuration> error(e.throwable());
                }
                if (retryExceptions.size() > 0) {
                    for (Class<? extends Throwable> cls : retryExceptions) {
                        if (e.throwable().getClass().isAssignableFrom(cls))
                            return Observable.just(e);
                    }
                    return Observable.error(e.throwable());
                } else {
                    return Observable.just(e);
                }
            }
        };

        Func1<Observable<? extends Throwable>, Observable<?>> notificationHandler = new Func1<Observable<? extends Throwable>, Observable<?>>() {

            @Override
            public Observable<ErrorAndDuration> call(Observable<? extends Throwable> errors) {
                final Action1<ErrorAndDuration> action2 = new Action1<ErrorAndDuration>() {

                    @Override
                    public void call(ErrorAndDuration e) {
                        if (e.durationMs() != NO_MORE_WAITS)
                            action.call(e);
                    }

                };
                return errors
                        // zip with waits, use -1 to signal completion
                        .zipWith(waits.concatWith(just(NO_MORE_WAITS)), TO_ERROR_AND_WAIT)
                        .flatMap(checkExceptions)
                        // perform user action (for example log that a
                        // wait is happening)
                        .doOnNext(action2)
                        // wait the time in ErrorAndWait
                        .flatMap(RetryWhen.wait(scheduler));
            }
        };
        return notificationHandler;
    }

    private final static Func2<Throwable, Long, ErrorAndDuration> TO_ERROR_AND_WAIT = new Func2<Throwable, Long, ErrorAndDuration>() {
        @Override
        public ErrorAndDuration call(Throwable throwable, Long waitMs) {
            return new ErrorAndDuration(throwable, waitMs);
        }
    };

    private static Func1<ErrorAndDuration, Observable<ErrorAndDuration>> wait(
            final Scheduler scheduler) {
        return new Func1<ErrorAndDuration, Observable<ErrorAndDuration>>() {
            @Override
            public Observable<ErrorAndDuration> call(ErrorAndDuration e) {
                if (e.durationMs() == NO_MORE_WAITS)
                    return Observable.error(e.throwable());
                else
                    return Observable.timer(e.durationMs(), TimeUnit.MILLISECONDS, scheduler)
                            .map(Functions.constant(e));
            }
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final List<Class<? extends Throwable>> retryExceptions = new ArrayList<Class<? extends Throwable>>();
        private final List<Class<? extends Throwable>> failExceptions = new ArrayList<Class<? extends Throwable>>();
        private Func1<? super Throwable, Boolean> exceptionPredicate = Functions.alwaysTrue();

        private Optional<Observable<Long>> waits = absent();
        private Optional<Integer> maxRetries = absent();
        private Optional<Scheduler> scheduler = of(Schedulers.computation());
        private Action1<? super ErrorAndDuration> action = Actions.doNothing1();

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

        public Builder waits(Observable<Long> waits, TimeUnit unit) {
            this.waits = of(waits.map(toMillis(unit)));
            return this;
        }

        public Builder wait(Long wait, final TimeUnit unit) {
            this.waits = of(Observable.just(wait).map(toMillis(unit)));
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

        public Builder exponentialBackoff(final long firstWait, final TimeUnit unit,
                final double factor) {
            waits = of(Observable.range(1, Integer.MAX_VALUE)
                    // make exponential
                    .map(new Func1<Integer, Long>() {
                        @Override
                        public Long call(Integer n) {
                            return Math.round(Math.pow(factor, n - 1) * unit.toMillis(firstWait));
                        }
                    }));
            return this;
        }

        public Builder exponentialBackoff(long wait, TimeUnit unit) {
            return exponentialBackoff(wait, unit, 2);
        }

        public Func1<Observable<? extends Throwable>, Observable<?>> build() {
            Preconditions.checkArgument(waits.isPresent());
            if (maxRetries.isPresent()) {
                waits = of(waits.get().take(maxRetries.get()));
            }
            return notificationHandler(waits.get(), scheduler.get(), action, retryExceptions,
                    failExceptions, exceptionPredicate);
        }

    }
}
