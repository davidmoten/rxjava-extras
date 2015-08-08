package com.github.davidmoten.rx;

import static rx.Observable.just;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.util.ErrorAndDuration;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

public class Retry {

    private static final long NO_MORE_WAITS = -1;

    static Func1<Observable<? extends Throwable>, Observable<?>> notificationHandler(
            final Observable<Long> waits, final Scheduler scheduler,
            final Action1<? super ErrorAndDuration> action) {
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
                        // perform user action (for example log that a
                        // wait is happening)
                        .doOnNext(action2)
                        // wait the time in ErrorAndWait
                        .flatMap(Retry.wait(scheduler));
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

}
