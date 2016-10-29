package com.github.davidmoten.rx.internal.operators;

import java.util.Iterator;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

public final class ObservableReverse {

    @SuppressWarnings("unchecked")
    public static <T> Observable<T> reverse(Observable<T> source) {
        return source.toList().flatMap((Func1<List<T>, Observable<T>>)(Func1<?,?>) REVERSE_LIST);
    }

    private static final Func1<List<Object>, Observable<Object>> REVERSE_LIST = new Func1<List<Object>, Observable<Object>>() {
        @Override
        public Observable<Object> call(List<Object> list) {
            return Observable.from(reverse(list));
        }
    };

    private static <T> Iterable<T> reverse(final List<T> list) {
        return new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {

                    int i = list.size();

                    @Override
                    public boolean hasNext() {
                        return i > 0;
                    }

                    @Override
                    public T next() {
                        i--;
                        return list.get(i);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                    
                };
            }
        };
    }

}
