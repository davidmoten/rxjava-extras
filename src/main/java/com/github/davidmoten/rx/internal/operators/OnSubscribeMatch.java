package com.github.davidmoten.rx.internal.operators;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.github.davidmoten.util.Preconditions;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.internal.operators.BackpressureUtils;

public final class OnSubscribeMatch<A, B, K, C> implements OnSubscribe<C> {

    private final Observable<A> a;
    private final Observable<B> b;
    private final Func1<? super A, ? extends K> aKey;
    private final Func1<? super B, ? extends K> bKey;
    private final Func2<? super A, ? super B, C> combiner;

    public OnSubscribeMatch(Observable<A> a, Observable<B> b, Func1<? super A, ? extends K> aKey,
            Func1<? super B, ? extends K> bKey, Func2<? super A, ? super B, C> combiner) {
        Preconditions.checkNotNull(a, "a should not be null");
        Preconditions.checkNotNull(b, "b should not be null");
        Preconditions.checkNotNull(aKey, "aKey cannot be null");
        Preconditions.checkNotNull(bKey, "bKey cannot be null");
        Preconditions.checkNotNull(combiner, "combiner cannot be null");
        this.a = a;
        this.b = b;
        this.aKey = aKey;
        this.bKey = bKey;
        this.combiner = combiner;

    }

    @Override
    public void call(Subscriber<? super C> child) {
        MyProducer<A, B, K, C> producer = new MyProducer<A, B, K, C>(a, b, aKey, bKey, combiner, child);
        int bufferSize = 128;
        MySubscriber<A, K> aSub = new MySubscriber<A, K>(bufferSize, Source.A, producer);
        MySubscriber<B, K> bSub = new MySubscriber<B, K>(bufferSize, Source.B, producer);
        child.add(aSub);
        child.add(bSub);
        child.setProducer(producer);
        a.unsafeSubscribe(aSub);
        b.unsafeSubscribe(bSub);
    }

    @SuppressWarnings("serial")
    private static final class MyProducer<A, B, K, C> extends AtomicLong implements Producer, Receiver {

        private final Queue<Object> queue = new LinkedList<Object>();
        private final Map<K, Queue<A>> as = new ConcurrentHashMap<K, Queue<A>>();
        private final Map<K, Queue<B>> bs = new ConcurrentHashMap<K, Queue<B>>();
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
            this.aKey = aKey;
            this.bKey = bKey;
            this.combiner = combiner;
            this.child = child;
            this.aSub = new MySubscriber<A, K>(bufferSize, Source.A, this);
            this.bSub = new MySubscriber<B, K>(bufferSize, Source.B, this);
        }

        @Override
        public void request(long n) {
            if (BackpressureUtils.validate(n)) {
                BackpressureUtils.getAndAddRequest(this, n);
                drain();
            }
        }

        void drain() {
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
                while (r > 0 && !queue.isEmpty()) {
                    if (child.isUnsubscribed()) {
                        return;
                    }
                    Object v = queue.poll();
                    if (v != null) {
                        if (v instanceof Item) {
                            Item item = (Item) v;
                            handle(item);
                        } else if (v instanceof Error) {
                            queue.clear();
                            as.clear();
                            bs.clear();
                            aSub.unsubscribe();
                            bSub.unsubscribe();
                            child.onError(((Error) v).error);
                            return;
                        } else if (v instanceof Completed) {
                            as.clear();
                            bs.clear();
                            aSub.unsubscribe();
                            bSub.unsubscribe();
                            queue.clear();
                            child.onCompleted();
                        }
                    } else {
                        break;
                    }
                    r--;
                    if (r == 0) {
                        r = get();
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

        private void handle(Item item) {
            if (item.source == Source.A) {
                @SuppressWarnings("unchecked")
                A a = (A) item.value;
                K key = aKey.call(a);
                Queue<B> q = bs.get(key);
                if (q == null) {
                    Queue<A> q2 = as.get(key);
                    if (q2 == null) {
                        q2 = new LinkedList<A>();
                        as.put(key, q2);
                    }
                    q2.offer(a);
                } else {
                    B b = q.poll();
                    if (q.isEmpty()) {
                        bs.remove(key);
                    }
                    C c = combiner.call(a, b);
                    child.onNext(c);
                }
                aSub.requestMore(1);
            } else {
                @SuppressWarnings("unchecked")
                B b = (B) item.value;
                K key = bKey.call(b);
                Queue<A> q = as.get(key);
                if (q == null) {
                    Queue<B> q2 = bs.get(key);
                    if (q2 == null) {
                        q2 = new LinkedList<B>();
                        bs.put(key, q2);
                    }
                    q2.offer(b);
                } else {
                    A a = q.poll();
                    if (q.isEmpty()) {
                        as.remove(key);
                    }
                    C c = combiner.call(a, b);
                    child.onNext(c);
                }
                bSub.requestMore(1);
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

        MySubscriber(int bufferSize, Source source, Receiver receiver) {
            this.bufferSize = bufferSize;
            this.source = source;
            this.receiver = receiver;
            request(bufferSize);
        }

        @Override
        public void onNext(T t) {
            receiver.offer(new Item(t, source));
        }

        @Override
        public void onCompleted() {
            receiver.offer(new Completed(source));
        }

        @Override
        public void onError(Throwable e) {
            receiver.offer(new Error(e, source));
        }

        public void requestMore(long n) {
            request(n);
        }

    }

    static final class Item {
        final Object value;
        final Source source;

        Item(Object value, Source source) {
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
