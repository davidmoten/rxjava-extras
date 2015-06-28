package com.github.davidmoten.rx;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.AbstractOnSubscribe;

public final class Serialized {

    public static <T extends Serializable> Observable<T> from(final InputStream is) {
        return Observable.create(new AbstractOnSubscribe<T, ObjectInputStream>() {

            @Override
            protected ObjectInputStream onSubscribe(Subscriber<? super T> subscriber) {
                try {
                    return new ObjectInputStream(is);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void next(SubscriptionState<T, ObjectInputStream> state) {
                ObjectInputStream ois = state.state();
                try {
                    T t = (T) ois.readObject();
                    state.onNext(t);
                } catch (ClassNotFoundException e) {
                    state.onError(e);
                    return;
                } catch (EOFException e) {
                    state.onCompleted();
                } catch (IOException e) {
                    state.onError(e);
                    return;
                }
            }
        });
    }

    public static <T extends Serializable> Observable<T> from(final File file) {
        Func0<InputStream> resourceFactory = new Func0<InputStream>() {
            @Override
            public InputStream call() {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Func1<InputStream, Observable<? extends T>> observableFactory = new Func1<InputStream, Observable<? extends T>>() {

            @Override
            public Observable<? extends T> call(InputStream is) {
                return from(is);
            }
        };
        Action1<InputStream> disposeAction = new Action1<InputStream>() {

            @Override
            public void call(InputStream is) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }};
        return Observable.using(resourceFactory, observableFactory, disposeAction, true);
    }

}
