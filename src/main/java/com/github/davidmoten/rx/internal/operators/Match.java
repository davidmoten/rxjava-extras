package com.github.davidmoten.rx.internal.operators;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.BehaviorSubject;

public final class Match {

    public static <A, B, C, K> Observable<C> match(final Observable<A> a, final Observable<B> b,
            final Func1<? super A, ? extends K> aKey, final Func1<? super B, ? extends K> bKey,
            final Func2<? super A, ? super B, C> combiner) {
        return Observable.defer(new Func0<Observable<C>>() {
            @Override
            public Observable<C> call() {
                // used to have manual control of `a` emissions
                final BehaviorSubject<Object> ar = BehaviorSubject.create();
                // used to have manual control of `b` emissions
                final BehaviorSubject<Object> br = BehaviorSubject.create();
                final Map<K, Queue<A>> map = new HashMap<K, Queue<A>>();
                Observable<A> a2 = a //
                        .zipWith(
                                Observable.<Object> just(1) //
                                        .concatWith(ar), //
                                new Func2<A, Object, A>() {
                                    @Override
                                    public A call(A x, Object y) {
                                        return x;
                                    }
                                });
                Observable<B> b2 = b //
                        .zipWith( //
                                Observable.<Object> just(1) //
                                        .concatWith(br), //
                                new Func2<B, Object, B>() {
                                    @Override
                                    public B call(B x, Object y) {
                                        return x;
                                    }
                                });
                return Observable //
                        .combineLatest(a2, b2, new Func2<A, B, Pair<A, B>>() {
                            @Override
                            public Pair<A, B> call(A x, B y) {
                                return new Pair<A, B>(x, y);
                            }
                        }) //
                        .flatMap(new Func1<Pair<A, B>, Observable<C>>() {
                            
                            @Override
                            public Observable<C> call(Pair<A, B> pair) {
                                K ak = aKey.call(pair.left);
                                Queue<A> q = map.computeIfAbsent(ak, new Function<K, Queue<A>>() {
                                    @Override
                                    public Queue<A> apply(K k) {
                                        return new LinkedList<A>();
                                    }
                                });
                                q.offer(pair.left);
                                K bk = bKey.call(pair.right);
                                Queue<A> q2 = map.get(bk);
                                final A v;
                                if (q2 == null)
                                    v = null;
                                else {
                                    v = q2.poll();
                                    if (q2.isEmpty()) {
                                        map.remove(bk);
                                    }
                                }
                                if (v != null) {
                                    final boolean empty = map.isEmpty();
                                    return Observable.just(combiner.call(v, pair.right)) //
                                            .doOnCompleted(new Action0() {
                                                @Override
                                                public void call() {
                                                    if (empty)
                                                        ar.onNext(1);
                                                    br.onNext(1);
                                                }
                                            });
                                } else {
                                    return Observable.<C> empty() //
                                            .doOnCompleted(new Action0() {
                                                @Override
                                                public void call() {
                                                    ar.onNext(1);
                                                }
                                            });
                                }
                            }
                        }, 1);
            }
        });
    }

    static class Pair<T, R> {

        final T left;
        final R right;

        Pair(T left, R right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Pair [left=");
            builder.append(left);
            builder.append(", right=");
            builder.append(right);
            builder.append("]");
            return builder.toString();
        }

    }
}
