package com.github.davidmoten.rx.internal.operators;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.internal.operators.BackpressureUtils;

public class OnSubscribeMatch<A, B, K, C> implements OnSubscribe<C> {

    private final Observable<A> a;
    private final Observable<B> b;
    private final Func1<? super A, ? extends K> aKey;
    private final Func1<? super B, ? extends K> bKey;
    private final Func2<? super A, ? super B, C> combiner;

    public OnSubscribeMatch(Observable<A> a, Observable<B> b, Func1<? super A, ? extends K> aKey,
            Func1<? super B, ? extends K> bKey, Func2<? super A, ? super B, C> combiner) {
        this.a = a;
        this.b = b;
        this.aKey = aKey;
        this.bKey = bKey;
        this.combiner = combiner;

    }

    @Override
    public void call(Subscriber<? super C> child) {
        child.setProducer(new MyProducer(a, b, aKey, bKey, combiner, child));
    }

    @SuppressWarnings("serial")
    private static final class MyProducer<A, B, K, C> extends AtomicLong implements Producer, Receiver {

        private final Queue<Object> queue = new LinkedList<Object>();
        private final Map<K, Queue<A>> as = new ConcurrentHashMap<K, Queue<A>>();
        private final Map<K, Queue<B>> bs = new ConcurrentHashMap<K, Queue<B>>();
        private final Observable<A> a;
        private final Observable<B> b;
        private final Func1<? super A, ? extends K> aKey;
        private final Func1<? super B, ? extends K> bKey;
        private final Func2<? super A, ? super B, C> combiner;
        private final Subscriber<? super C> child;
        private final int bufferSize = 128;
        private final MySubscriber<A, K> aSub;
        private final MySubscriber<B, K> bSub;
        private int wip = 0;

        MyProducer(Observable<A> a, Observable<B> b, Func1<? super A, ? extends K> aKey,
                Func1<? super B, ? extends K> bKey, Func2<? super A, ? super B, C> combiner,
                Subscriber<? super C> child) {
            this.a = a;
            this.b = b;
            this.aKey = aKey;
            this.bKey = bKey;
            this.combiner = combiner;
            this.child = child;
            this.aSub = new MySubscriber<A, K>(bufferSize, Source.A, this);
            this.bSub = new MySubscriber<B, K>(bufferSize, Source.B, this);
            a.unsafeSubscribe(aSub);
            b.unsafeSubscribe(bSub);
        }

        @Override
        public void request(long n) {
            if (n == 0) {
                return;
            } else if (n < 0) {
                throw new IllegalArgumentException("request must be >=0");
            } else if (BackpressureUtils.getAndAddRequest(this, n) == 0) {
                drain();
            }
        }

        public void drain() {
            synchronized (this) {
                if (wip > 0) {
                    wip++;
                    return;
                } else {
                    wip = 1;
                }
            }
            while (true) {
                long r = get();
                while (r > 0) {
                    if (child.isUnsubscribed()) {
                        return;
                    }
                    Object v = queue.poll();
                    if (v != null) {
                        if (v instanceof Item) {
                           
                        } else if (v instanceof Error) {
                            
                        } else if (v instanceof Completed) {
                            
                        }
                    } else {
                        break;
                    }
                }

                synchronized (this) {
                    wip--;
                    if (wip == 0) {
                        return;
                    }
                    wip = 1;
                }
            }
        }

        @Override
        public void offer(Object item) {
            queue.offer(item);
            drain();
        }

    }

    interface Receiver {
        void offer(Object item);

    }

    @SuppressWarnings("unused")
    private static class MySubscriber<T, K> extends Subscriber<T> {

        private final int bufferSize;
        private final Receiver receiver;
        private final Source source;
        volatile boolean completed = false;
        volatile Throwable error;

        public MySubscriber(int bufferSize, Source source, Receiver receiver) {
            this.bufferSize = bufferSize;
            this.source = source;
            this.receiver = receiver;
            request(bufferSize);
        }

        @Override
        public void onNext(T t) {
            receiver.offer(new Item<Object>(t, source));
        }

        @Override
        public void onCompleted() {
            receiver.offer(new Completed(source));
        }

        @Override
        public void onError(Throwable e) {
            receiver.offer(new Error(e, source));
        }

    }

    static final class Item<T> {
        final T value;
        final Source source;

        Item(T value, Source source) {
            super();
            this.value = value;
            this.source = source;
        }

    }

    static final class Completed {
        final Source source;

        Completed(Source source) {
            this.source = source;
        }

    }

    static final class Error {
        final Throwable error;
        final Source source;

        Error(Throwable error, Source source) {
            this.error = error;
            this.source = source;
        }

    }

    enum Source {
        A, B;
    }
}
