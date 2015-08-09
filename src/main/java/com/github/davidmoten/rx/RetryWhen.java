package com.github.davidmoten.rx;

import static com.github.davidmoten.util.Optional.of;
import static rx.Observable.just;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import com.github.davidmoten.util.ErrorAndDuration;
import com.github.davidmoten.util.Optional;

public class RetryWhen {

    private static final long NO_MORE_WAITS = -1;

    static Func1<Observable<? extends Throwable>, Observable<?>> notificationHandler(
            final Observable<Long> waits, final Scheduler scheduler,
            final Action1<? super ErrorAndDuration> action,
            final List<Class<? extends Throwable>> retryExceptions,
            final List<Class<? extends Throwable>> failExceptions) {
        final Func1<ErrorAndDuration, Observable<ErrorAndDuration>> checkExceptions = new Func1<ErrorAndDuration, Observable<ErrorAndDuration>>() {

            @Override
            public Observable<ErrorAndDuration> call(ErrorAndDuration e) {
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
                    return Observable.timer(e.durationMs(), TimeUnit.MILLISECONDS, scheduler).map(
                            Functions.constant(e));
            }
        };
    }

    public static class Builder {

        private final List<Class<? extends Throwable>> retryWhen = new ArrayList<Class<? extends Throwable>>();
        private final List<Class<? extends Throwable>> failWhen = new ArrayList<Class<? extends Throwable>>();
        private Optional<Func1<Throwable, Boolean>> exceptionPredicate = Optional.absent();
        private Optional<Observable<Long>> waits;
        private Optional<TimeUnit> unit;
        private Optional<Integer> maxRetries;
        private Optional<Scheduler> scheduler;
        private Action1<? super ErrorAndDuration> action;
        private double factor;

        public Builder retryWhenInstanceOf(Class<? extends Throwable>... classes) {
            retryWhen.addAll(Arrays.asList(classes));
            return this;
        }

        public Builder failWhenInstanceOf(Class<? extends Throwable>... classes) {
            failWhen.addAll(Arrays.asList(classes));
            return this;
        }

        public Builder retryIf(Func1<Throwable, Boolean> predicate) {
            this.exceptionPredicate = Optional.of(predicate);
            return this;
        }

        public Builder waits(Observable<Long> waits, TimeUnit unit) {
            this.waits = of(waits);
            this.unit = of(unit);
            return this;
        }

        public Builder wait(Long wait, TimeUnit unit) {
            this.waits = of(Observable.just(wait));
            return this;
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

        public Builder exponentialBackoff(long wait, TimeUnit unit, double factor) {
            wait(wait, unit);
            this.factor = factor;
            return this;
        }

        public Builder exponentialBackoff(long wait, TimeUnit unit) {
            return exponentialBackoff(wait, unit, 2);
        }

    }
}
