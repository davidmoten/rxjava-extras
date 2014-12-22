package com.github.davidmoten.rx;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.functions.Func1;

public class TransformationTester {

    public static <T, R> Builder<T, R> function(Func1<Observable<T>, Observable<R>> function) {
        return new Builder<T, R>().function(function);
    }

    public static <T, R> ExpectBuilder<T, R> fromEmpty() {
        return new ExpectBuilder<T, R>(true);
    }

    public static <T, R> ExpectBuilder<T, R> from(T... items) {
        return new ExpectBuilder<T, R>(Arrays.asList(items));
    }

    public static class Builder<T, R> {

        private Func1<Observable<T>, Observable<R>> function;

        public ExpectBuilder<T, R> fromEmpty() {
            // TODO add expects
            return TransformationTester.fromEmpty();
        }

        public ExpectBuilder<T, R> from(T... items) {
            // TODO add expects
            return TransformationTester.<T, R> from(items);
        }

        public Builder<T, R> function(Func1<Observable<T>, Observable<R>> function) {
            this.function = function;
            return this;
        }
    }

    public static class ExpectBuilder<T, R> {
        private final boolean empty;
        private List<T> list;

        private ExpectBuilder(List<T> list) {
            this.list = list;
            this.empty = false;
        }

        private ExpectBuilder(boolean empty) {
            this.empty = empty;
        }

        public Builder<T, R> expectEmpty() {
            return new Builder<T, R>();
        }

        public Builder<T, R> expect(R... items) {
            return expect(Arrays.asList(items));
        }

        public Builder<T, R> expect(List<R> items) {
            return new Builder<T, R>();
        }

        public Builder<T, R> expect(Set<R> set) {
            return new Builder<T, R>();
        }
    }

    public static void main(String[] args) {
        // test count operator
        Func1<Observable<String>, Observable<Integer>> f = new Func1<Observable<String>, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Observable<String> o) {
                return o.count();
            }
        };
        TransformationTester.function(f)
        // test empty
                .fromEmpty().expect(0)
                // test non-empty count
                .from("a", "b").expect(2);

    }
}
