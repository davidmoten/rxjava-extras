package com.github.davidmoten.rx.operators;

import rx.Notification;
import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Producer;
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
        Requester<T> requester = new Requester<T>(subscribers);
        EventSubscriber<T> eventSubscriber = new EventSubscriber<T>(child, comparator, requester);
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
        child.setProducer(requester);
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
        private final Requester<T> requester;

        public EventSubscriber(Subscriber<? super T> child,
                Func2<? super T, ? super T, Integer> comparator, Requester<T> requester) {
            this.child = child;
            this.comparator = comparator;
            this.requester = requester;
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
                    requester.requestOne(event.subscriberIndex);
                } else if (buffer == EMPTY_SENTINEL) {
                    buffer = value;
                    bufferSubscriberIndex = event.subscriberIndex;
                } else {
                    if (comparator.call(value, buffer) <= 0) {
                        child.onNext(value);
                        requester.requestOne(event.subscriberIndex);
                    } else {
                        child.onNext(buffer);
                        int requestFrom = bufferSubscriberIndex;
                        buffer = value;
                        bufferSubscriberIndex = event.subscriberIndex;
                        requester.requestOne(requestFrom);
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
                    requester.requestOne(other(event.subscriberIndex));
                }

            }
        }

        private int other(int subscriberIndex) {
            return (subscriberIndex + 1) % 2;
        }
    }

    private static final class Requester<T> implements Producer {

        private final MergeSubscriber<T>[] subscribers;
        private long requested = 0;
        private int requestFromIndex = -1;

        Requester(MergeSubscriber<T>[] subscribers) {
            this.subscribers = subscribers;
        }

        @Override
        public void request(long n) {
            if (n <= 0)
                return;
            int reqFrom;
            synchronized (this) {
                requested += n;
                if (requested < 0) {
                    requested = Long.MAX_VALUE;
                }
                reqFrom = requestFromIndex;
                requestFromIndex = -1;
            }
            if (reqFrom >= 0) {
                subscribers[reqFrom].requestOne();
            }
        }

        void requestOne(int subscriberIndex) {
            boolean canRequest;
            synchronized (this) {
                canRequest = requested > 0;
            }
            if (canRequest) {
                subscribers[subscriberIndex].requestOne();
                synchronized (this) {
                    requestFromIndex = -1;
                    if (requested != Long.MAX_VALUE)
                        requested--;
                }
            } else {
                synchronized (this) {
                    if (requestFromIndex >= 0)
                        throw new RuntimeException("requesting too much");
                    requestFromIndex = subscriberIndex;
                }
            }
        }
    }
}
