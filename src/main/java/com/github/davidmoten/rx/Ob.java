package com.github.davidmoten.rx;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Notification;
import rx.Observable;
import rx.Observable.Operator;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Scheduler;
import rx.Single;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.BlockingObservable;
import rx.observables.ConnectableObservable;
import rx.observables.GroupedObservable;
import rx.schedulers.TimeInterval;
import rx.schedulers.Timestamped;

public class Ob<T> {

    private final Observable<T> o;

    @Override
    public int hashCode() {
        return o.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return o.equals(obj);
    }

    public final <R> Observable<R> lift(Operator<? extends R, ? super T> lift) {
        return o.lift(lift);
    }

    public <R> Observable<R> compose(Transformer<? super T, ? extends R> transformer) {
        return o.compose(transformer);
    }

    public Single<T> toSingle() {
        return o.toSingle();
    }

    @Override
    public String toString() {
        return o.toString();
    }

    public final Observable<Observable<T>> nest() {
        return o.nest();
    }

    public final Observable<Boolean> all(Func1<? super T, Boolean> predicate) {
        return o.all(predicate);
    }

    public final Observable<T> ambWith(Observable<? extends T> t1) {
        return o.ambWith(t1);
    }

    public final Observable<T> asObservable() {
        return o.asObservable();
    }

    public final <TClosing> Observable<List<T>> buffer(
            Func0<? extends Observable<? extends TClosing>> bufferClosingSelector) {
        return o.buffer(bufferClosingSelector);
    }

    public final Observable<List<T>> buffer(int count) {
        return o.buffer(count);
    }

    public final Observable<List<T>> buffer(int count, int skip) {
        return o.buffer(count, skip);
    }

    public final Observable<List<T>> buffer(long timespan, long timeshift, TimeUnit unit) {
        return o.buffer(timespan, timeshift, unit);
    }

    public final Observable<List<T>> buffer(long timespan, long timeshift, TimeUnit unit,
            Scheduler scheduler) {
        return o.buffer(timespan, timeshift, unit, scheduler);
    }

    public final Observable<List<T>> buffer(long timespan, TimeUnit unit) {
        return o.buffer(timespan, unit);
    }

    public final Observable<List<T>> buffer(long timespan, TimeUnit unit, int count) {
        return o.buffer(timespan, unit, count);
    }

    public final Observable<List<T>> buffer(long timespan, TimeUnit unit, int count,
            Scheduler scheduler) {
        return o.buffer(timespan, unit, count, scheduler);
    }

    public final Observable<List<T>> buffer(long timespan, TimeUnit unit, Scheduler scheduler) {
        return o.buffer(timespan, unit, scheduler);
    }

    public final <TOpening, TClosing> Observable<List<T>> buffer(
            Observable<? extends TOpening> bufferOpenings,
            Func1<? super TOpening, ? extends Observable<? extends TClosing>> bufferClosingSelector) {
        return o.buffer(bufferOpenings, bufferClosingSelector);
    }

    public final <B> Observable<List<T>> buffer(Observable<B> boundary) {
        return o.buffer(boundary);
    }

    public final <B> Observable<List<T>> buffer(Observable<B> boundary, int initialCapacity) {
        return o.buffer(boundary, initialCapacity);
    }

    public final Observable<T> cache() {
        return o.cache();
    }

    public final Observable<T> cache(int capacityHint) {
        return o.cache(capacityHint);
    }

    public final <R> Observable<R> cast(Class<R> klass) {
        return o.cast(klass);
    }

    public final <R> Observable<R> collect(Func0<R> stateFactory, Action2<R, ? super T> collector) {
        return o.collect(stateFactory, collector);
    }

    public final <R> Observable<R> concatMap(
            Func1<? super T, ? extends Observable<? extends R>> func) {
        return o.concatMap(func);
    }

    public final Observable<T> concatWith(Observable<? extends T> t1) {
        return o.concatWith(t1);
    }

    public final Observable<Boolean> contains(Object element) {
        return o.contains(element);
    }

    public final Observable<Integer> count() {
        return o.count();
    }

    public final Observable<Long> countLong() {
        return o.countLong();
    }

    public final <U> Observable<T> debounce(
            Func1<? super T, ? extends Observable<U>> debounceSelector) {
        return o.debounce(debounceSelector);
    }

    public final Observable<T> debounce(long timeout, TimeUnit unit) {
        return o.debounce(timeout, unit);
    }

    public final Observable<T> debounce(long timeout, TimeUnit unit, Scheduler scheduler) {
        return o.debounce(timeout, unit, scheduler);
    }

    public final Observable<T> defaultIfEmpty(T defaultValue) {
        return o.defaultIfEmpty(defaultValue);
    }

    public final Observable<T> switchIfEmpty(Observable<? extends T> alternate) {
        return o.switchIfEmpty(alternate);
    }

    public final <U, V> Observable<T> delay(Func0<? extends Observable<U>> subscriptionDelay,
            Func1<? super T, ? extends Observable<V>> itemDelay) {
        return o.delay(subscriptionDelay, itemDelay);
    }

    public final <U> Observable<T> delay(Func1<? super T, ? extends Observable<U>> itemDelay) {
        return o.delay(itemDelay);
    }

    public final Observable<T> delay(long delay, TimeUnit unit) {
        return o.delay(delay, unit);
    }

    public final Observable<T> delay(long delay, TimeUnit unit, Scheduler scheduler) {
        return o.delay(delay, unit, scheduler);
    }

    public final Observable<T> delaySubscription(long delay, TimeUnit unit) {
        return o.delaySubscription(delay, unit);
    }

    public final Observable<T> delaySubscription(long delay, TimeUnit unit, Scheduler scheduler) {
        return o.delaySubscription(delay, unit, scheduler);
    }

    public final <U> Observable<T> delaySubscription(
            Func0<? extends Observable<U>> subscriptionDelay) {
        return o.delaySubscription(subscriptionDelay);
    }

    public final <T2> Observable<T2> dematerialize() {
        return o.dematerialize();
    }

    public final Observable<T> distinct() {
        return o.distinct();
    }

    public final <U> Observable<T> distinct(Func1<? super T, ? extends U> keySelector) {
        return o.distinct(keySelector);
    }

    public final Observable<T> distinctUntilChanged() {
        return o.distinctUntilChanged();
    }

    public final <U> Observable<T> distinctUntilChanged(Func1<? super T, ? extends U> keySelector) {
        return o.distinctUntilChanged(keySelector);
    }

    public final Observable<T> doOnCompleted(Action0 onCompleted) {
        return o.doOnCompleted(onCompleted);
    }

    public final Observable<T> doOnEach(Action1<Notification<? super T>> onNotification) {
        return o.doOnEach(onNotification);
    }

    public final Observable<T> doOnEach(Observer<? super T> observer) {
        return o.doOnEach(observer);
    }

    public final Observable<T> doOnError(Action1<Throwable> onError) {
        return o.doOnError(onError);
    }

    public final Observable<T> doOnNext(Action1<? super T> onNext) {
        return o.doOnNext(onNext);
    }

    public final Observable<T> doOnRequest(Action1<Long> onRequest) {
        return o.doOnRequest(onRequest);
    }

    public final Observable<T> doOnSubscribe(Action0 subscribe) {
        return o.doOnSubscribe(subscribe);
    }

    public final Observable<T> doOnTerminate(Action0 onTerminate) {
        return o.doOnTerminate(onTerminate);
    }

    public final Observable<T> doOnUnsubscribe(Action0 unsubscribe) {
        return o.doOnUnsubscribe(unsubscribe);
    }

    public final Observable<T> elementAt(int index) {
        return o.elementAt(index);
    }

    public final Observable<T> elementAtOrDefault(int index, T defaultValue) {
        return o.elementAtOrDefault(index, defaultValue);
    }

    public final Observable<Boolean> exists(Func1<? super T, Boolean> predicate) {
        return o.exists(predicate);
    }

    public final Observable<T> filter(Func1<? super T, Boolean> predicate) {
        return o.filter(predicate);
    }

    public final Observable<T> finallyDo(Action0 action) {
        return o.finallyDo(action);
    }

    public final Observable<T> first() {
        return o.first();
    }

    public final Observable<T> first(Func1<? super T, Boolean> predicate) {
        return o.first(predicate);
    }

    public final Observable<T> firstOrDefault(T defaultValue) {
        return o.firstOrDefault(defaultValue);
    }

    public final Observable<T> firstOrDefault(T defaultValue, Func1<? super T, Boolean> predicate) {
        return o.firstOrDefault(defaultValue, predicate);
    }

    public final <R> Observable<R> flatMap(
            Func1<? super T, ? extends Observable<? extends R>> func) {
        return o.flatMap(func);
    }

    public final <R> Observable<R> flatMap(Func1<? super T, ? extends Observable<? extends R>> func,
            int maxConcurrent) {
        return o.flatMap(func, maxConcurrent);
    }

    public final <R> Observable<R> flatMap(
            Func1<? super T, ? extends Observable<? extends R>> onNext,
            Func1<? super Throwable, ? extends Observable<? extends R>> onError,
            Func0<? extends Observable<? extends R>> onCompleted) {
        return o.flatMap(onNext, onError, onCompleted);
    }

    public final <R> Observable<R> flatMap(
            Func1<? super T, ? extends Observable<? extends R>> onNext,
            Func1<? super Throwable, ? extends Observable<? extends R>> onError,
            Func0<? extends Observable<? extends R>> onCompleted, int maxConcurrent) {
        return o.flatMap(onNext, onError, onCompleted, maxConcurrent);
    }

    public final <U, R> Observable<R> flatMap(
            Func1<? super T, ? extends Observable<? extends U>> collectionSelector,
            Func2<? super T, ? super U, ? extends R> resultSelector) {
        return o.flatMap(collectionSelector, resultSelector);
    }

    public final <U, R> Observable<R> flatMap(
            Func1<? super T, ? extends Observable<? extends U>> collectionSelector,
            Func2<? super T, ? super U, ? extends R> resultSelector, int maxConcurrent) {
        return o.flatMap(collectionSelector, resultSelector, maxConcurrent);
    }

    public final <R> Observable<R> flatMapIterable(
            Func1<? super T, ? extends Iterable<? extends R>> collectionSelector) {
        return o.flatMapIterable(collectionSelector);
    }

    public final <U, R> Observable<R> flatMapIterable(
            Func1<? super T, ? extends Iterable<? extends U>> collectionSelector,
            Func2<? super T, ? super U, ? extends R> resultSelector) {
        return o.flatMapIterable(collectionSelector, resultSelector);
    }

    public final void forEach(Action1<? super T> onNext) {
        o.forEach(onNext);
    }

    public final void forEach(Action1<? super T> onNext, Action1<Throwable> onError) {
        o.forEach(onNext, onError);
    }

    public final void forEach(Action1<? super T> onNext, Action1<Throwable> onError,
            Action0 onComplete) {
        o.forEach(onNext, onError, onComplete);
    }

    public final <K, R> Observable<GroupedObservable<K, R>> groupBy(
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends R> elementSelector) {
        return o.groupBy(keySelector, elementSelector);
    }

    public final <K> Observable<GroupedObservable<K, T>> groupBy(
            Func1<? super T, ? extends K> keySelector) {
        return o.groupBy(keySelector);
    }

    public final <T2, D1, D2, R> Observable<R> groupJoin(Observable<T2> right,
            Func1<? super T, ? extends Observable<D1>> leftDuration,
            Func1<? super T2, ? extends Observable<D2>> rightDuration,
            Func2<? super T, ? super Observable<T2>, ? extends R> resultSelector) {
        return o.groupJoin(right, leftDuration, rightDuration, resultSelector);
    }

    public final Observable<T> ignoreElements() {
        return o.ignoreElements();
    }

    public final Observable<Boolean> isEmpty() {
        return o.isEmpty();
    }

    public final <TRight, TLeftDuration, TRightDuration, R> Observable<R> join(
            Observable<TRight> right, Func1<T, Observable<TLeftDuration>> leftDurationSelector,
            Func1<TRight, Observable<TRightDuration>> rightDurationSelector,
            Func2<T, TRight, R> resultSelector) {
        return o.join(right, leftDurationSelector, rightDurationSelector, resultSelector);
    }

    public final Observable<T> last() {
        return o.last();
    }

    public final Observable<T> last(Func1<? super T, Boolean> predicate) {
        return o.last(predicate);
    }

    public final Observable<T> lastOrDefault(T defaultValue) {
        return o.lastOrDefault(defaultValue);
    }

    public final Observable<T> lastOrDefault(T defaultValue, Func1<? super T, Boolean> predicate) {
        return o.lastOrDefault(defaultValue, predicate);
    }

    public final Observable<T> limit(int count) {
        return o.limit(count);
    }

    public final <R> Observable<R> map(Func1<? super T, ? extends R> func) {
        return o.map(func);
    }

    public final Observable<Notification<T>> materialize() {
        return o.materialize();
    }

    public final Observable<T> mergeWith(Observable<? extends T> t1) {
        return o.mergeWith(t1);
    }

    public final Observable<T> observeOn(Scheduler scheduler) {
        return o.observeOn(scheduler);
    }

    public final <R> Observable<R> ofType(Class<R> klass) {
        return o.ofType(klass);
    }

    public final Observable<T> onBackpressureBuffer() {
        return o.onBackpressureBuffer();
    }

    public final Observable<T> onBackpressureBuffer(long capacity) {
        return o.onBackpressureBuffer(capacity);
    }

    public final Observable<T> onBackpressureBuffer(long capacity, Action0 onOverflow) {
        return o.onBackpressureBuffer(capacity, onOverflow);
    }

    public final Observable<T> onBackpressureDrop(Action1<? super T> onDrop) {
        return o.onBackpressureDrop(onDrop);
    }

    public final Observable<T> onBackpressureDrop() {
        return o.onBackpressureDrop();
    }

    public final Observable<T> onBackpressureBlock(int maxQueueLength) {
        return o.onBackpressureBlock(maxQueueLength);
    }

    public final Observable<T> onBackpressureBlock() {
        return o.onBackpressureBlock();
    }

    public final Observable<T> onBackpressureLatest() {
        return o.onBackpressureLatest();
    }

    public final Observable<T> onErrorResumeNext(
            Func1<Throwable, ? extends Observable<? extends T>> resumeFunction) {
        return o.onErrorResumeNext(resumeFunction);
    }

    public final Observable<T> onErrorResumeNext(Observable<? extends T> resumeSequence) {
        return o.onErrorResumeNext(resumeSequence);
    }

    public final Observable<T> onErrorReturn(Func1<Throwable, ? extends T> resumeFunction) {
        return o.onErrorReturn(resumeFunction);
    }

    public final Observable<T> onExceptionResumeNext(Observable<? extends T> resumeSequence) {
        return o.onExceptionResumeNext(resumeSequence);
    }

    public final ConnectableObservable<T> publish() {
        return o.publish();
    }

    public final <R> Observable<R> publish(
            Func1<? super Observable<T>, ? extends Observable<R>> selector) {
        return o.publish(selector);
    }

    public final Observable<T> reduce(Func2<T, T, T> accumulator) {
        return o.reduce(accumulator);
    }

    public final <R> Observable<R> reduce(R initialValue, Func2<R, ? super T, R> accumulator) {
        return o.reduce(initialValue, accumulator);
    }

    public final Observable<T> repeat() {
        return o.repeat();
    }

    public final Observable<T> repeat(Scheduler scheduler) {
        return o.repeat(scheduler);
    }

    public final Observable<T> repeat(long count) {
        return o.repeat(count);
    }

    public final Observable<T> repeat(long count, Scheduler scheduler) {
        return o.repeat(count, scheduler);
    }

    public final Observable<T> repeatWhen(
            Func1<? super Observable<? extends Void>, ? extends Observable<?>> notificationHandler,
            Scheduler scheduler) {
        return o.repeatWhen(notificationHandler, scheduler);
    }

    public final Observable<T> repeatWhen(
            Func1<? super Observable<? extends Void>, ? extends Observable<?>> notificationHandler) {
        return o.repeatWhen(notificationHandler);
    }

    public final ConnectableObservable<T> replay() {
        return o.replay();
    }

    public final <R> Observable<R> replay(
            Func1<? super Observable<T>, ? extends Observable<R>> selector) {
        return o.replay(selector);
    }

    public final <R> Observable<R> replay(
            Func1<? super Observable<T>, ? extends Observable<R>> selector, int bufferSize) {
        return o.replay(selector, bufferSize);
    }

    public final <R> Observable<R> replay(
            Func1<? super Observable<T>, ? extends Observable<R>> selector, int bufferSize,
            long time, TimeUnit unit) {
        return o.replay(selector, bufferSize, time, unit);
    }

    public final <R> Observable<R> replay(
            Func1<? super Observable<T>, ? extends Observable<R>> selector, int bufferSize,
            long time, TimeUnit unit, Scheduler scheduler) {
        return o.replay(selector, bufferSize, time, unit, scheduler);
    }

    public final <R> Observable<R> replay(
            Func1<? super Observable<T>, ? extends Observable<R>> selector, int bufferSize,
            Scheduler scheduler) {
        return o.replay(selector, bufferSize, scheduler);
    }

    public final <R> Observable<R> replay(
            Func1<? super Observable<T>, ? extends Observable<R>> selector, long time,
            TimeUnit unit) {
        return o.replay(selector, time, unit);
    }

    public final <R> Observable<R> replay(
            Func1<? super Observable<T>, ? extends Observable<R>> selector, long time,
            TimeUnit unit, Scheduler scheduler) {
        return o.replay(selector, time, unit, scheduler);
    }

    public final <R> Observable<R> replay(
            Func1<? super Observable<T>, ? extends Observable<R>> selector, Scheduler scheduler) {
        return o.replay(selector, scheduler);
    }

    public final ConnectableObservable<T> replay(int bufferSize) {
        return o.replay(bufferSize);
    }

    public final ConnectableObservable<T> replay(int bufferSize, long time, TimeUnit unit) {
        return o.replay(bufferSize, time, unit);
    }

    public final ConnectableObservable<T> replay(int bufferSize, long time, TimeUnit unit,
            Scheduler scheduler) {
        return o.replay(bufferSize, time, unit, scheduler);
    }

    public final ConnectableObservable<T> replay(int bufferSize, Scheduler scheduler) {
        return o.replay(bufferSize, scheduler);
    }

    public final ConnectableObservable<T> replay(long time, TimeUnit unit) {
        return o.replay(time, unit);
    }

    public final ConnectableObservable<T> replay(long time, TimeUnit unit, Scheduler scheduler) {
        return o.replay(time, unit, scheduler);
    }

    public final ConnectableObservable<T> replay(Scheduler scheduler) {
        return o.replay(scheduler);
    }

    public final Observable<T> retry() {
        return o.retry();
    }

    public final Observable<T> retry(long count) {
        return o.retry(count);
    }

    public final Observable<T> retry(Func2<Integer, Throwable, Boolean> predicate) {
        return o.retry(predicate);
    }

    public final Observable<T> retryWhen(
            Func1<? super Observable<? extends Throwable>, ? extends Observable<?>> notificationHandler) {
        return o.retryWhen(notificationHandler);
    }

    public final Observable<T> retryWhen(
            Func1<? super Observable<? extends Throwable>, ? extends Observable<?>> notificationHandler,
            Scheduler scheduler) {
        return o.retryWhen(notificationHandler, scheduler);
    }

    public final Observable<T> sample(long period, TimeUnit unit) {
        return o.sample(period, unit);
    }

    public final Observable<T> sample(long period, TimeUnit unit, Scheduler scheduler) {
        return o.sample(period, unit, scheduler);
    }

    public final <U> Observable<T> sample(Observable<U> sampler) {
        return o.sample(sampler);
    }

    public final Observable<T> scan(Func2<T, T, T> accumulator) {
        return o.scan(accumulator);
    }

    public final <R> Observable<R> scan(R initialValue, Func2<R, ? super T, R> accumulator) {
        return o.scan(initialValue, accumulator);
    }

    public final Observable<T> serialize() {
        return o.serialize();
    }

    public final Observable<T> share() {
        return o.share();
    }

    public final Observable<T> single() {
        return o.single();
    }

    public final Observable<T> single(Func1<? super T, Boolean> predicate) {
        return o.single(predicate);
    }

    public final Observable<T> singleOrDefault(T defaultValue) {
        return o.singleOrDefault(defaultValue);
    }

    public final Observable<T> singleOrDefault(T defaultValue,
            Func1<? super T, Boolean> predicate) {
        return o.singleOrDefault(defaultValue, predicate);
    }

    public final Observable<T> skip(int count) {
        return o.skip(count);
    }

    public final Observable<T> skip(long time, TimeUnit unit) {
        return o.skip(time, unit);
    }

    public final Observable<T> skip(long time, TimeUnit unit, Scheduler scheduler) {
        return o.skip(time, unit, scheduler);
    }

    public final Observable<T> skipLast(int count) {
        return o.skipLast(count);
    }

    public final Observable<T> skipLast(long time, TimeUnit unit) {
        return o.skipLast(time, unit);
    }

    public final Observable<T> skipLast(long time, TimeUnit unit, Scheduler scheduler) {
        return o.skipLast(time, unit, scheduler);
    }

    public final <U> Observable<T> skipUntil(Observable<U> other) {
        return o.skipUntil(other);
    }

    public final Observable<T> skipWhile(Func1<? super T, Boolean> predicate) {
        return o.skipWhile(predicate);
    }

    public final Observable<T> startWith(Observable<T> values) {
        return o.startWith(values);
    }

    public final Observable<T> startWith(Iterable<T> values) {
        return o.startWith(values);
    }

    public final Observable<T> startWith(T t1) {
        return o.startWith(t1);
    }

    public final Observable<T> startWith(T t1, T t2) {
        return o.startWith(t1, t2);
    }

    public final Observable<T> startWith(T t1, T t2, T t3) {
        return o.startWith(t1, t2, t3);
    }

    public final Observable<T> startWith(T t1, T t2, T t3, T t4) {
        return o.startWith(t1, t2, t3, t4);
    }

    public final Observable<T> startWith(T t1, T t2, T t3, T t4, T t5) {
        return o.startWith(t1, t2, t3, t4, t5);
    }

    public final Observable<T> startWith(T t1, T t2, T t3, T t4, T t5, T t6) {
        return o.startWith(t1, t2, t3, t4, t5, t6);
    }

    public final Observable<T> startWith(T t1, T t2, T t3, T t4, T t5, T t6, T t7) {
        return o.startWith(t1, t2, t3, t4, t5, t6, t7);
    }

    public final Observable<T> startWith(T t1, T t2, T t3, T t4, T t5, T t6, T t7, T t8) {
        return o.startWith(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    public final Observable<T> startWith(T t1, T t2, T t3, T t4, T t5, T t6, T t7, T t8, T t9) {
        return o.startWith(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    public final Subscription subscribe() {
        return o.subscribe();
    }

    public final Subscription subscribe(Action1<? super T> onNext) {
        return o.subscribe(onNext);
    }

    public final Subscription subscribe(Action1<? super T> onNext, Action1<Throwable> onError) {
        return o.subscribe(onNext, onError);
    }

    public final Subscription subscribe(Action1<? super T> onNext, Action1<Throwable> onError,
            Action0 onComplete) {
        return o.subscribe(onNext, onError, onComplete);
    }

    public final Subscription subscribe(Observer<? super T> observer) {
        return o.subscribe(observer);
    }

    public final Subscription unsafeSubscribe(Subscriber<? super T> subscriber) {
        return o.unsafeSubscribe(subscriber);
    }

    public final Subscription subscribe(Subscriber<? super T> subscriber) {
        return o.subscribe(subscriber);
    }

    public final Observable<T> subscribeOn(Scheduler scheduler) {
        return o.subscribeOn(scheduler);
    }

    public final <R> Observable<R> switchMap(
            Func1<? super T, ? extends Observable<? extends R>> func) {
        return o.switchMap(func);
    }

    public final Observable<T> take(int count) {
        return o.take(count);
    }

    public final Observable<T> take(long time, TimeUnit unit) {
        return o.take(time, unit);
    }

    public final Observable<T> take(long time, TimeUnit unit, Scheduler scheduler) {
        return o.take(time, unit, scheduler);
    }

    public final Observable<T> takeFirst(Func1<? super T, Boolean> predicate) {
        return o.takeFirst(predicate);
    }

    public final Observable<T> takeLast(int count) {
        return o.takeLast(count);
    }

    public final Observable<T> takeLast(int count, long time, TimeUnit unit) {
        return o.takeLast(count, time, unit);
    }

    public final Observable<T> takeLast(int count, long time, TimeUnit unit, Scheduler scheduler) {
        return o.takeLast(count, time, unit, scheduler);
    }

    public final Observable<T> takeLast(long time, TimeUnit unit) {
        return o.takeLast(time, unit);
    }

    public final Observable<T> takeLast(long time, TimeUnit unit, Scheduler scheduler) {
        return o.takeLast(time, unit, scheduler);
    }

    public final Observable<List<T>> takeLastBuffer(int count) {
        return o.takeLastBuffer(count);
    }

    public final Observable<List<T>> takeLastBuffer(int count, long time, TimeUnit unit) {
        return o.takeLastBuffer(count, time, unit);
    }

    public final Observable<List<T>> takeLastBuffer(int count, long time, TimeUnit unit,
            Scheduler scheduler) {
        return o.takeLastBuffer(count, time, unit, scheduler);
    }

    public final Observable<List<T>> takeLastBuffer(long time, TimeUnit unit) {
        return o.takeLastBuffer(time, unit);
    }

    public final Observable<List<T>> takeLastBuffer(long time, TimeUnit unit, Scheduler scheduler) {
        return o.takeLastBuffer(time, unit, scheduler);
    }

    public final <E> Observable<T> takeUntil(Observable<? extends E> other) {
        return o.takeUntil(other);
    }

    public final Observable<T> takeWhile(Func1<? super T, Boolean> predicate) {
        return o.takeWhile(predicate);
    }

    public final Observable<T> takeUntil(Func1<? super T, Boolean> stopPredicate) {
        return o.takeUntil(stopPredicate);
    }

    public final Observable<T> throttleFirst(long windowDuration, TimeUnit unit) {
        return o.throttleFirst(windowDuration, unit);
    }

    public final Observable<T> throttleFirst(long skipDuration, TimeUnit unit,
            Scheduler scheduler) {
        return o.throttleFirst(skipDuration, unit, scheduler);
    }

    public final Observable<T> throttleLast(long intervalDuration, TimeUnit unit) {
        return o.throttleLast(intervalDuration, unit);
    }

    public final Observable<T> throttleLast(long intervalDuration, TimeUnit unit,
            Scheduler scheduler) {
        return o.throttleLast(intervalDuration, unit, scheduler);
    }

    public final Observable<T> throttleWithTimeout(long timeout, TimeUnit unit) {
        return o.throttleWithTimeout(timeout, unit);
    }

    public final Observable<T> throttleWithTimeout(long timeout, TimeUnit unit,
            Scheduler scheduler) {
        return o.throttleWithTimeout(timeout, unit, scheduler);
    }

    public final Observable<TimeInterval<T>> timeInterval() {
        return o.timeInterval();
    }

    public final Observable<TimeInterval<T>> timeInterval(Scheduler scheduler) {
        return o.timeInterval(scheduler);
    }

    public final <U, V> Observable<T> timeout(Func0<? extends Observable<U>> firstTimeoutSelector,
            Func1<? super T, ? extends Observable<V>> timeoutSelector) {
        return o.timeout(firstTimeoutSelector, timeoutSelector);
    }

    public final <U, V> Observable<T> timeout(Func0<? extends Observable<U>> firstTimeoutSelector,
            Func1<? super T, ? extends Observable<V>> timeoutSelector,
            Observable<? extends T> other) {
        return o.timeout(firstTimeoutSelector, timeoutSelector, other);
    }

    public final <V> Observable<T> timeout(
            Func1<? super T, ? extends Observable<V>> timeoutSelector) {
        return o.timeout(timeoutSelector);
    }

    public final <V> Observable<T> timeout(
            Func1<? super T, ? extends Observable<V>> timeoutSelector,
            Observable<? extends T> other) {
        return o.timeout(timeoutSelector, other);
    }

    public final Observable<T> timeout(long timeout, TimeUnit timeUnit) {
        return o.timeout(timeout, timeUnit);
    }

    public final Observable<T> timeout(long timeout, TimeUnit timeUnit,
            Observable<? extends T> other) {
        return o.timeout(timeout, timeUnit, other);
    }

    public final Observable<T> timeout(long timeout, TimeUnit timeUnit,
            Observable<? extends T> other, Scheduler scheduler) {
        return o.timeout(timeout, timeUnit, other, scheduler);
    }

    public final Observable<T> timeout(long timeout, TimeUnit timeUnit, Scheduler scheduler) {
        return o.timeout(timeout, timeUnit, scheduler);
    }

    public final Observable<Timestamped<T>> timestamp() {
        return o.timestamp();
    }

    public final Observable<Timestamped<T>> timestamp(Scheduler scheduler) {
        return o.timestamp(scheduler);
    }

    public final BlockingObservable<T> toBlocking() {
        return o.toBlocking();
    }

    public final Observable<List<T>> toList() {
        return o.toList();
    }

    public final <K> Observable<Map<K, T>> toMap(Func1<? super T, ? extends K> keySelector) {
        return o.toMap(keySelector);
    }

    public final <K, V> Observable<Map<K, V>> toMap(Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector) {
        return o.toMap(keySelector, valueSelector);
    }

    public final <K, V> Observable<Map<K, V>> toMap(Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector, Func0<? extends Map<K, V>> mapFactory) {
        return o.toMap(keySelector, valueSelector, mapFactory);
    }

    public final <K> Observable<Map<K, Collection<T>>> toMultimap(
            Func1<? super T, ? extends K> keySelector) {
        return o.toMultimap(keySelector);
    }

    public final <K, V> Observable<Map<K, Collection<V>>> toMultimap(
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector) {
        return o.toMultimap(keySelector, valueSelector);
    }

    public final <K, V> Observable<Map<K, Collection<V>>> toMultimap(
            Func1<? super T, ? extends K> keySelector, Func1<? super T, ? extends V> valueSelector,
            Func0<? extends Map<K, Collection<V>>> mapFactory) {
        return o.toMultimap(keySelector, valueSelector, mapFactory);
    }

    public final <K, V> Observable<Map<K, Collection<V>>> toMultimap(
            Func1<? super T, ? extends K> keySelector, Func1<? super T, ? extends V> valueSelector,
            Func0<? extends Map<K, Collection<V>>> mapFactory,
            Func1<? super K, ? extends Collection<V>> collectionFactory) {
        return o.toMultimap(keySelector, valueSelector, mapFactory, collectionFactory);
    }

    public final Observable<List<T>> toSortedList() {
        return o.toSortedList();
    }

    public final Observable<List<T>> toSortedList(
            Func2<? super T, ? super T, Integer> sortFunction) {
        return o.toSortedList(sortFunction);
    }

    public final Observable<List<T>> toSortedList(int initialCapacity) {
        return o.toSortedList(initialCapacity);
    }

    public final Observable<List<T>> toSortedList(Func2<? super T, ? super T, Integer> sortFunction,
            int initialCapacity) {
        return o.toSortedList(sortFunction, initialCapacity);
    }

    public final Observable<T> unsubscribeOn(Scheduler scheduler) {
        return o.unsubscribeOn(scheduler);
    }

    public final <U, R> Observable<R> withLatestFrom(Observable<? extends U> other,
            Func2<? super T, ? super U, ? extends R> resultSelector) {
        return o.withLatestFrom(other, resultSelector);
    }

    public final <TClosing> Observable<Observable<T>> window(
            Func0<? extends Observable<? extends TClosing>> closingSelector) {
        return o.window(closingSelector);
    }

    public final Observable<Observable<T>> window(int count) {
        return o.window(count);
    }

    public final Observable<Observable<T>> window(int count, int skip) {
        return o.window(count, skip);
    }

    public final Observable<Observable<T>> window(long timespan, long timeshift, TimeUnit unit) {
        return o.window(timespan, timeshift, unit);
    }

    public final Observable<Observable<T>> window(long timespan, long timeshift, TimeUnit unit,
            Scheduler scheduler) {
        return o.window(timespan, timeshift, unit, scheduler);
    }

    public final Observable<Observable<T>> window(long timespan, long timeshift, TimeUnit unit,
            int count, Scheduler scheduler) {
        return o.window(timespan, timeshift, unit, count, scheduler);
    }

    public final Observable<Observable<T>> window(long timespan, TimeUnit unit) {
        return o.window(timespan, unit);
    }

    public final Observable<Observable<T>> window(long timespan, TimeUnit unit, int count) {
        return o.window(timespan, unit, count);
    }

    public final Observable<Observable<T>> window(long timespan, TimeUnit unit, int count,
            Scheduler scheduler) {
        return o.window(timespan, unit, count, scheduler);
    }

    public final Observable<Observable<T>> window(long timespan, TimeUnit unit,
            Scheduler scheduler) {
        return o.window(timespan, unit, scheduler);
    }

    public final <TOpening, TClosing> Observable<Observable<T>> window(
            Observable<? extends TOpening> windowOpenings,
            Func1<? super TOpening, ? extends Observable<? extends TClosing>> closingSelector) {
        return o.window(windowOpenings, closingSelector);
    }

    public final <U> Observable<Observable<T>> window(Observable<U> boundary) {
        return o.window(boundary);
    }

    public final <T2, R> Observable<R> zipWith(Iterable<? extends T2> other,
            Func2<? super T, ? super T2, ? extends R> zipFunction) {
        return o.zipWith(other, zipFunction);
    }

    public final <T2, R> Observable<R> zipWith(Observable<? extends T2> other,
            Func2<? super T, ? super T2, ? extends R> zipFunction) {
        return o.zipWith(other, zipFunction);
    }

    protected Ob(rx.Observable.OnSubscribe<T> f) {
        this.o = Observable.create(f);
    }

}
