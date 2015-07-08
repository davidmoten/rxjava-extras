package com.github.davidmoten.rx.operators;

import java.util.concurrent.atomic.AtomicReference;

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
        final AtomicReference<MergeSubscriber<T>> mainRef = new AtomicReference<MergeSubscriber<T>>();
        final AtomicReference<MergeSubscriber<T>> otherRef = new AtomicReference<MergeSubscriber<T>>();
        final EventSubscriber<T> eventSubscriber = new EventSubscriber<T>(child, comparator,
                mainRef, otherRef);
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
        child.setProducer(new Producer() {

            @Override
            public void request(long n) {
                eventSubscriber.requestMore(n);
            }
        });
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

        void requestOne() {
            request(1);
        }

        @Override
        public void onStart() {
            requestOne();
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
        private final ProducerObserver<T> producerObserver;

        EventSubscriber(Subscriber<? super T> child,
                Func2<? super T, ? super T, Integer> comparator,
                AtomicReference<MergeSubscriber<T>> mainRef,
                AtomicReference<MergeSubscriber<T>> otherRef) {
            this.child = child;
            this.comparator = comparator;
            this.mainRef = mainRef;
            this.otherRef = otherRef;
            this.producerObserver = new ProducerObserver<T>(child);
        }

        void requestMore(long n) {
            producerObserver.request(n);
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
                    producerObserver.onNext(value, event.subscriber);
                } else if (buffer == EMPTY_SENTINEL) {
                    buffer = value;
                } else {
                    if (comparator.call(value, buffer) <= 0) {
                        producerObserver.onNext(value, event.subscriber);
                    } else {
                        producerObserver.onNext(buffer, other(event));
                        buffer = value;
                    }
                }
            } else if (event.notification.isOnCompleted()) {
                completedCount += 1;
                if (buffer != EMPTY_SENTINEL) {
                    producerObserver.onNext(buffer, other(event));
                    buffer = (T) EMPTY_SENTINEL;
                }
                if (completedCount == 2) {
                    producerObserver.onCompleted();
                } else {
                    producerObserver.onCompleted(other(event));
                }

            }
        }

        private MergeSubscriber<T> other(Event<T> event) {
            if (mainRef.get() == event.subscriber) {
                return otherRef.get();
            } else
                return mainRef.get();
        }
    }

    private static final class ProducerObserver<T> implements Producer {

        private final Subscriber<? super T> child;
        @SuppressWarnings("unchecked")
        private final T EMPTY = (T) new Object();
        private long expected;
        private boolean completed;
        private T value = EMPTY;
        private boolean busy = false;
        private int counter = 0;
        private MergeSubscriber<T> requestFrom;

        ProducerObserver(Subscriber<? super T> child) {
            this.child = child;
        }

        @Override
        public void request(long n) {
            if (n <= 0)
                return;
            synchronized (this) {
                expected += n;
                if (expected < 0)
                    expected = Long.MAX_VALUE;
                if (busy) {
                    counter++;
                    return;
                } else
                    busy = true;
            }
            drain();
        }

        void onCompleted(MergeSubscriber<T> other) {
            other.requestOne();
        }

        void onCompleted() {
            synchronized (this) {
                completed = true;
                if (busy) {
                    counter++;
                    return;
                } else
                    busy = true;
            }
            drain();
        }

        void onError(Throwable e) {
            child.onError(e);
        }

        void onNext(T t, MergeSubscriber<T> requestFrom) {
            synchronized (this) {
                if (value != EMPTY) {
                    throw new RuntimeException(
                            "onNext has arrived without being requested. OrderedMerge source observables must support backpressure!");
                }
                value = t;
                this.requestFrom = requestFrom;
                if (busy) {
                    counter++;
                    return;
                } else
                    busy = true;
            }
            drain();
        }

        private void drain() {
            try {
                long r;
                synchronized (this) {
                    r = expected;
                }
                while (true) {
                    synchronized (this) {
                        counter = 1;
                    }
                    if (!child.isUnsubscribed()) {
                        if (value == EMPTY && completed) {
                            // don't need to check requested to complete
                            child.onCompleted();
                            return;
                        } else if (r > 0) {
                            if (value != EMPTY) {
                                child.onNext(value);
                            }
                            if (completed) {
                                child.onCompleted();
                                return;
                            } else if (value != EMPTY) {
                                requestFrom.requestOne();
                            }
                        }
                    }
                    synchronized (this) {
                        if (--counter == 0) {
                            return;
                        } else {
                            r = --expected;
                        }
                    }
                }
            } finally {
                synchronized (this) {
                    busy = false;
                }
            }
        }
    }
}
