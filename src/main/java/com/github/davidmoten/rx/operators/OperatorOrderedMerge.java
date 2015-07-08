package com.github.davidmoten.rx.operators;

import java.util.concurrent.atomic.AtomicReference;

import rx.Notification;
import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func2;
import rx.observers.SerializedSubscriber;

import com.github.davidmoten.rx.subjects.PublishSubjectSingleSubscriber;

public class OperatorOrderedMerge<T> implements Operator<T, T> {

    private final Observable<T> other;
    private final Func2<? super T, ? super T, Integer> comparator;
    private static final Object EMPTY_SENTINEL = new Object();

    public OperatorOrderedMerge(Observable<T> other, Func2<? super T, ? super T, Integer> comparator) {
        this.other = other;
        this.comparator = comparator;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final PublishSubjectSingleSubscriber<Event<T>> subject = PublishSubjectSingleSubscriber
                .create();
        final AtomicReference<MergeSubscriber<T>> mainRef = new AtomicReference<MergeSubscriber<T>>();
        final AtomicReference<MergeSubscriber<T>> otherRef = new AtomicReference<MergeSubscriber<T>>();
        EventSubscriber<T> eventSubscriber = new EventSubscriber<T>(child, comparator, mainRef,
                otherRef);
        Subscriber<Event<T>> serializedEventSubscriber = new SerializedSubscriber<Event<T>>(
                eventSubscriber);
        MergeSubscriber<T> mainSubscriber = new MergeSubscriber<T>(serializedEventSubscriber);
        MergeSubscriber<T> otherSubscriber = new MergeSubscriber<T>(serializedEventSubscriber);
        mainRef.set(mainSubscriber);
        otherRef.set(otherSubscriber);
        child.add(mainSubscriber);
        child.add(otherSubscriber);
        child.add(serializedEventSubscriber);
        subject.unsafeSubscribe(serializedEventSubscriber);
        other.unsafeSubscribe(otherSubscriber);
        return mainSubscriber;
    }

    private static class Event<T> {
        final MergeSubscriber<T> subscriber;
        final Notification<T> notification;

        Event(MergeSubscriber<T> subscriber, Notification<T> notification) {
            this.subscriber = subscriber;
            this.notification = notification;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Event [subscriber=");
            builder.append(subscriber);
            builder.append(", notification=");
            builder.append(notification);
            builder.append("]");
            return builder.toString();
        }

    }

    private static class MergeSubscriber<T> extends Subscriber<T> {

        private final Observer<Event<T>> observer;

        MergeSubscriber(Observer<Event<T>> observer) {
            this.observer = observer;
        }

        void requestMore(long n) {
            request(n);
        }

        @Override
        public void onStart() {
            request(1);
        }

        @Override
        public void onCompleted() {
            observer.onNext(new Event<T>(this, Notification.<T> createOnCompleted()));
        }

        @Override
        public void onError(Throwable e) {
            observer.onError(e);
        }

        @Override
        public void onNext(T t) {
            observer.onNext(new Event<T>(this, Notification.<T> createOnNext(t)));
        }
    }

    private static final class EventSubscriber<T> extends Subscriber<Event<T>> {

        private final Subscriber<? super T> child;
        private final Func2<? super T, ? super T, Integer> comparator;
        private final AtomicReference<MergeSubscriber<T>> mainRef;
        private final AtomicReference<MergeSubscriber<T>> otherRef;

        public EventSubscriber(Subscriber<? super T> child,
                Func2<? super T, ? super T, Integer> comparator,
                AtomicReference<MergeSubscriber<T>> mainRef,
                AtomicReference<MergeSubscriber<T>> otherRef) {
            this.child = child;
            this.comparator = comparator;
            this.mainRef = mainRef;
            this.otherRef = otherRef;
        }

        @SuppressWarnings("unchecked")
        T buffer = (T) EMPTY_SENTINEL;
        int completedCount = 0;

        @Override
        public void onCompleted() {
            // should not get called
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onNext(Event<T> event) {
            // System.out.println("buffer = " + buffer + " " +
            // event);
            if (event.notification.hasValue()) {
                T value = event.notification.getValue();
                if (completedCount == 1) {
                    child.onNext(value);
                    event.subscriber.requestMore(1);
                } else if (buffer == EMPTY_SENTINEL) {
                    buffer = value;
                } else {
                    if (comparator.call(value, buffer) <= 0) {
                        child.onNext(value);
                        event.subscriber.requestMore(1);
                    } else {
                        child.onNext(buffer);
                        buffer = value;
                        requestFromOther(event);
                    }
                }
            } else if (event.notification.isOnCompleted()) {
                completedCount += 1;
                if (buffer != EMPTY_SENTINEL) {
                    child.onNext(buffer);
                    buffer = (T) EMPTY_SENTINEL;
                }
                if (completedCount == 2) {
                    child.onCompleted();
                } else {
                    // TODO may request more than required
                    requestFromOther(event);
                }

            }
        }

        private void requestFromOther(Event<T> event) {
            if (mainRef.get() == event.subscriber) {
                otherRef.get().requestMore(1);
            } else
                mainRef.get().requestMore(1);
        }
    }
}
