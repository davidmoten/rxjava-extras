package com.github.davidmoten.rx.internal.operators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.CompositeException;
import rx.exceptions.MissingBackpressureException;
import rx.internal.operators.BackpressureUtils;
import rx.internal.operators.NotificationLite;
import rx.internal.util.RxRingBuffer;
import rx.internal.util.unsafe.MpscLinkedQueue;

/**
 * @author David Karnokd
 *
 * @param <T>
 *            type of observable
 */
public final class OrderedMerge<T> implements OnSubscribe<T> {
    final List<Observable<T>> sources;
    final Comparator<? super T> comparator;
    final boolean delayErrors;

    public static <U extends Comparable<? super U>> Observable<U> create(
            Collection<Observable<U>> sources) {
        return create(sources, false);
    }

    public static <U> Observable<U> create(Collection<Observable<U>> sources,
            Comparator<? super U> comparator) {
        return create(sources, comparator, false);
    }

    public static <U extends Comparable<? super U>> Observable<U> create(
            Collection<Observable<U>> sources, boolean delayErrors) {
        return Observable.create(new OrderedMerge<U>(sources, new Comparator<U>() {
            @Override
            public int compare(U o1, U o2) {
                return o1.compareTo(o2);
            }
        }, delayErrors));
    }

    public static <U> Observable<U> create(Collection<Observable<U>> sources,
            Comparator<? super U> comparator, boolean delayErrors) {
        return Observable.create(new OrderedMerge<U>(sources, comparator, delayErrors));
    }

    private OrderedMerge(Collection<Observable<T>> sources,
            Comparator<? super T> comparator, boolean delayErrors) {
        this.sources = sources instanceof List ? (List<Observable<T>>) sources
                : new ArrayList<Observable<T>>(sources);
        this.comparator = comparator;
        this.delayErrors = delayErrors;
    }

    @Override
    public void call(Subscriber<? super T> child) {
        @SuppressWarnings("unchecked")
        SourceSubscriber<T>[] sources = new SourceSubscriber[this.sources.size()];
        MergeProducer<T> mp = new MergeProducer<T>(sources, child, comparator, delayErrors);
        for (int i = 0; i < sources.length; i++) {
            if (child.isUnsubscribed()) {
                return;
            }
            SourceSubscriber<T> s = new SourceSubscriber<T>(mp);
            sources[i] = s;
            child.add(s);
        }
        mp.set(0); // release contents of the array
        child.setProducer(mp);
        int i = 0;
        for (Observable<? extends T> source : this.sources) {
            if (child.isUnsubscribed()) {
                return;
            }
            source.unsafeSubscribe(sources[i]);
            i++;
        }
    }

    static final class MergeProducer<T> extends AtomicLong implements Producer {
        /** */
        private static final long serialVersionUID = -812969080497027108L;

        final NotificationLite<T> nl = NotificationLite.instance();

        final boolean delayErrors;
        final Comparator<? super T> comparator;
        @SuppressWarnings("rawtypes")
        final SourceSubscriber[] sources;
        final Subscriber<? super T> child;

        final Queue<Throwable> errors;

        boolean emitting;
        boolean missed;

        @SuppressWarnings("rawtypes")
        public MergeProducer(SourceSubscriber[] sources, Subscriber<? super T> child,
                Comparator<? super T> comparator, boolean delayErrors) {
            this.sources = sources;
            this.delayErrors = delayErrors;
            this.errors = new MpscLinkedQueue<Throwable>();
            this.child = child;
            this.comparator = comparator;
        }

        @Override
        public void request(long n) {
            BackpressureUtils.getAndAddRequest(this, n);
            emit();
        }

        public void error(Throwable ex) {
            errors.offer(ex);
            emit();
        }

        public void emit() {
            synchronized (this) {
                if (emitting) {
                    missed = true;
                    return;
                }
                emitting = true;
            }
            // lift into local variables, just in case
            @SuppressWarnings("unchecked")
            final SourceSubscriber<T>[] sources = this.sources;
            final int n = sources.length;
            final Subscriber<? super T> child = this.child;

            for (;;) {
                if (child.isUnsubscribed()) {
                    return;
                }
                // eagerly check for errors
                if (!delayErrors && !errors.isEmpty()) {
                    child.onError(errors.poll());
                    return;
                }
                // the current requested
                long r = get();
                // aggregate total emissions
                long e = 0;
                // even without request, terminal events can be fired if the
                // state is right
                if (r == 0) {
                    int doneCount = 0;
                    // for each source
                    for (SourceSubscriber<T> s : sources) {
                        // if completed earlier
                        if (s == null) {
                            doneCount++;
                        } else {
                            // or just completed
                            if (s.done && s.queue.isEmpty()) {
                                doneCount++;
                            }
                        }
                    }
                    // if all of them are completed
                    if (doneCount == n) {
                        reportErrorOrComplete(child);
                        return;
                    }
                }
                // until there is request
                while (r != 0L) {
                    if (child.isUnsubscribed()) {
                        return;
                    }
                    // eagerly check for errors
                    if (!delayErrors && !errors.isEmpty()) {
                        child.onError(errors.poll());
                        return;
                    }
                    // indicates that every active source has at least one value
                    boolean fullRow = true;
                    // indicates that at least one value is available
                    boolean hasAtLeastOne = false;
                    // holds the smallest of the available values
                    T minimum = null;
                    // indicates which source's value is taken so it can be
                    // polled/replenished
                    int toPoll = -1;
                    // number of completed sources
                    int doneCount = 0;
                    // for each source
                    for (int i = 0; i < n; i++) {
                        SourceSubscriber<T> s = sources[i];
                        // terminated and emptied sources are ignored
                        if (s == null) {
                            doneCount++;
                            continue;
                        }
                        // read the terminal indicator first
                        boolean d = s.done;
                        // peek into the queue
                        Object o = s.queue.peek();
                        // no value available
                        if (o == null) {
                            // because it terminated?
                            if (d) {
                                sources[i] = null;
                                doneCount++;
                                continue;
                            }
                            // otherwise, indicate not all queues are ready
                            fullRow = false;
                            break;
                        }
                        // if we already found a value, compare it against the
                        // current
                        if (hasAtLeastOne) {
                            T v = nl.getValue(o);
                            int c = comparator.compare(minimum, v);
                            if (c > 0) {
                                minimum = v;
                                toPoll = i;
                            }
                        } else {
                            // this is the first value found
                            minimum = nl.getValue(o);
                            hasAtLeastOne = true;
                            toPoll = i;
                        }
                    }
                    // in case all of the sources completed
                    if (doneCount == n) {
                        reportErrorOrComplete(child);
                        return;
                    }
                    // if there was a full row of available values
                    if (fullRow) {
                        // given the winner
                        if (toPoll >= 0) {
                            SourceSubscriber<T> s = sources[toPoll];
                            // remove the winning value from its queue
                            s.queue.poll();
                            // request replenishment
                            s.requestMore(1);
                        }
                        // emit the smallest
                        child.onNext(minimum);
                        // decrement the available request and increment the
                        // emit count
                        if (r != Long.MAX_VALUE) {
                            r--;
                            e++;
                        }
                    } else {
                        // if some sources weren't ready, just quit
                        break;
                    }
                }

                // if there was emission, adjust the downstream request amount
                if (e != 0L) {
                    addAndGet(-e);
                }

                synchronized (this) {
                    if (!missed) {
                        emitting = false;
                        return;
                    }
                    missed = false;
                }
            }
        }

        void reportErrorOrComplete(Subscriber<? super T> child) {
            if (delayErrors && !errors.isEmpty()) {
                if (errors.size() == 1) {
                    child.onError(errors.poll());
                } else {
                    child.onError(new CompositeException(errors));
                }
            } else {
                child.onCompleted();
            }
        }
    }

    static final class SourceSubscriber<T> extends Subscriber<T> {
        final RxRingBuffer queue;
        final MergeProducer<T> parent;
        volatile boolean done;

        public SourceSubscriber(MergeProducer<T> parent) {
            queue = RxRingBuffer.getSpscInstance();
            this.parent = parent;
        }

        @Override
        public void onStart() {
            add(queue);
            request(RxRingBuffer.SIZE);
        }

        public void requestMore(long n) {
            request(n);
        }

        @Override
        public void onNext(T t) {
            try {
                queue.onNext(parent.nl.next(t));
            } catch (MissingBackpressureException mbe) {
                try {
                    onError(mbe);
                } finally {
                    unsubscribe();
                }
                return;
            } catch (IllegalStateException ex) {
                if (!isUnsubscribed()) {
                    try {
                        onError(ex);
                    } finally {
                        unsubscribe();
                    }
                }
                return;
            }
            parent.emit();
        }

        @Override
        public void onError(Throwable e) {
            done = true;
            parent.error(e);
        }

        @Override
        public void onCompleted() {
            done = true;
            parent.emit();
        }
    }
}