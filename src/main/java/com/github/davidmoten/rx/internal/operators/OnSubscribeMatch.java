package com.github.davidmoten.rx.internal.operators;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
    private static final int INITIAL_REQUEST = 128;

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
        AtomicReference<Receiver> receiverHolder = new AtomicReference<Receiver>();
        MySubscriber<A, K> aSub = new MySubscriber<A, K>(Source.A, receiverHolder);
        MySubscriber<B, K> bSub = new MySubscriber<B, K>(Source.B, receiverHolder);
        child.add(aSub);
        child.add(bSub);
        MyProducer<A, B, K, C> producer = new MyProducer<A, B, K, C>(a, b, aKey, bKey, combiner, aSub, bSub, child);
        receiverHolder.set(producer);
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
        private final MySubscriber<A, K> aSub;
        private final MySubscriber<B, K> bSub;
        private int wip = 0;
        private int completed = 0;

        MyProducer(Observable<A> a, Observable<B> b, Func1<? super A, ? extends K> aKey,
                Func1<? super B, ? extends K> bKey, Func2<? super A, ? super B, C> combiner, MySubscriber<A, K> aSub,
                MySubscriber<B, K> bSub, Subscriber<? super C> child) {
            this.aKey = aKey;
            this.bKey = bKey;
            this.combiner = combiner;
            this.child = child;
            this.aSub = aSub;
            this.bSub = bSub;
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
                int emitted = 0;
                while (r > emitted & !queue.isEmpty()) {
                    if (child.isUnsubscribed()) {
                        return;
                    }
                    Object v = queue.poll();
                    if (v != null) {
                        if (v instanceof Item) {
                            Item item = (Item) v;
                            emitted += emit(item);
                        } else if (v instanceof Error) {
                            queue.clear();
                            as.clear();
                            bs.clear();
                            aSub.unsubscribe();
                            bSub.unsubscribe();
                            child.onError(((Error) v).error);
                            return;
                        } else {
                            // completed
                            Completed comp = (Completed) v;
                            completed += 1;
                            if (comp.source == Source.A) {
                                aSub.unsubscribe();
                            } else {
                                bSub.unsubscribe();
                            }
                            if (completed == 2) {
                                as.clear();
                                bs.clear();
                                queue.clear();
                                child.onCompleted();
                            }
                        }
                    } else {
                        break;
                    }
                    if (r == emitted) {
                        r = addAndGet(-emitted);
                        emitted = 0;
                    }
                }
                if (emitted > 0) {
                    // queue was exhausted but requests were not
                    addAndGet(-emitted);
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

        private int emit(Item item) {
            int result = 0;
            if (item.source == Source.A) {
                @SuppressWarnings("unchecked")
                A a = (A) item.value;
                K key = aKey.call(a);
                Queue<B> q = bs.get(key);
                if (q == null) {
                    add(as, key, a);
                } else {
                    B b = poll(bs, q, key);
                    C c = combiner.call(a, b);
                    child.onNext(c);
                    result = 1;
                }
                aSub.requestMore(1);
            } else {
                @SuppressWarnings("unchecked")
                B b = (B) item.value;
                K key = bKey.call(b);
                Queue<A> q = as.get(key);
                if (q == null) {
                    add(bs, key, b);
                } else {
                    A a = poll(as, q, key);
                    C c = combiner.call(a, b);
                    child.onNext(c);
                    result = 1;
                }
                bSub.requestMore(1);
            }
            return result;
        }

        private static <K, T> void add(Map<K, Queue<T>> map, K key, T value) {
            Queue<T> q = map.get(key);
            if (q == null) {
                q = new LinkedList<T>();
                map.put(key, q);
            }
            q.offer(value);
        }

        private static <K, T> T poll(Map<K, Queue<T>> map, Queue<T> q, K key) {
            T t = q.poll();
            if (q.isEmpty()) {
                map.remove(key);
            }
            return t;
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

    private static class MySubscriber<T, K> extends Subscriber<T> {

        private final AtomicReference<Receiver> receiver;
        private final Source source;

        MySubscriber(Source source, AtomicReference<Receiver> receiver) {
            this.source = source;
            this.receiver = receiver;
            request(INITIAL_REQUEST);
        }

        @Override
        public void onNext(T t) {
            receiver.get().offer(new Item(t, source));
        }

        @Override
        public void onCompleted() {
            receiver.get().offer(new Completed(source));
        }

        @Override
        public void onError(Throwable e) {
            receiver.get().offer(new Error(e, source));
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
