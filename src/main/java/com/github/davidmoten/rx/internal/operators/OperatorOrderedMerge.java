package com.github.davidmoten.rx.internal.operators;

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
        @SuppressWarnings("unchecked")
        final MergeSubscriber<T>[] subscribers = new MergeSubscriber[2];
        EventSubscriber<T> eventSubscriber = new EventSubscriber<T>(child, comparator, subscribers);
        Subscriber<Event<T>> serializedEventSubscriber = new SerializedSubscriber<Event<T>>(
                eventSubscriber);
        MergeSubscriber<T> mainSubscriber = new MergeSubscriber<T>(serializedEventSubscriber, 0);
        MergeSubscriber<T> otherSubscriber = new MergeSubscriber<T>(serializedEventSubscriber, 1);
        child.add(mainSubscriber);
        synchronized (this) {
            subscribers[0] = mainSubscriber;
            subscribers[1] = otherSubscriber;
        }
        child.add(otherSubscriber);
        child.add(serializedEventSubscriber);
        subject.unsafeSubscribe(serializedEventSubscriber);
        other.unsafeSubscribe(otherSubscriber);
        return mainSubscriber;
    }

    private static class Event<T> {
        final int subscriberIndex;
        final Notification<T> notification;

        Event(int subscriberIndex, Notification<T> notification) {
            this.subscriberIndex = subscriberIndex;
            this.notification = notification;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Event [subscriber=");
            builder.append(subscriberIndex);
            builder.append(", notification=");
            builder.append(notification);
            builder.append("]");
            return builder.toString();
        }

    }

    private static class MergeSubscriber<T> extends Subscriber<T> {

        private final Observer<Event<T>> observer;
        private final int index;

        MergeSubscriber(Observer<Event<T>> observer, int index) {
            this.observer = observer;
            this.index = index;
        }

        void requestOne() {
            request(1);
        }

        @Override
        public void onStart() {
            request(1);
        }

        @Override
        public void onCompleted() {
            observer.onNext(new Event<T>(index, Notification.<T> createOnCompleted()));
        }

        @Override
        public void onError(Throwable e) {
            observer.onError(e);
        }

        @Override
        public void onNext(T t) {
            observer.onNext(new Event<T>(index, Notification.<T> createOnNext(t)));
        }

        @Override
        public String toString() {
            if (index == 0)
                return "main";
            else
                return "other";
        }
    }

    private static final class EventSubscriber<T> extends Subscriber<Event<T>> {

        private final Subscriber<? super T> child;
        private final Func2<? super T, ? super T, Integer> comparator;

        @SuppressWarnings("unchecked")
        private T buffer = (T) EMPTY_SENTINEL;
        private int bufferSubscriberIndex = -1;
        private int completedCount = 0;
        private final MergeSubscriber<T>[] subscribers;

        public EventSubscriber(Subscriber<? super T> child,
                Func2<? super T, ? super T, Integer> comparator, MergeSubscriber<T>[] subscribers) {
            this.child = child;
            this.comparator = comparator;
            this.subscribers = subscribers;
        }

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
            if (event.notification.hasValue()) {
                T value = event.notification.getValue();
                if (completedCount == 1 && buffer == EMPTY_SENTINEL) {
                    child.onNext(value);
                    subscribers[event.subscriberIndex].requestOne();
                } else if (buffer == EMPTY_SENTINEL) {
                    buffer = value;
                    bufferSubscriberIndex = event.subscriberIndex;
                } else {
                    if (comparator.call(value, buffer) <= 0) {
                        child.onNext(value);
                        subscribers[event.subscriberIndex].requestOne();
                    } else {
                        child.onNext(buffer);
                        int requestFrom = bufferSubscriberIndex;
                        buffer = value;
                        bufferSubscriberIndex = event.subscriberIndex;
                        subscribers[requestFrom].requestOne();
                    }
                }
            } else if (event.notification.isOnCompleted()) {
                completedCount += 1;
                if (completedCount == 2) {
                    if (buffer != EMPTY_SENTINEL) {
                        child.onNext(buffer);
                        buffer = (T) EMPTY_SENTINEL;
                    }
                    child.onCompleted();
                } else {
                    other(event.subscriberIndex).requestOne();
                }

            }
        }

        private MergeSubscriber<T> other(int subscriberIndex) {
            return subscribers[(subscriberIndex + 1) % 2];
        }
    }
}
