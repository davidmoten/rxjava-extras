package com.github.davidmoten.rx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.AbstractOnSubscribe;

public final class Serialized {

    public static <T extends Serializable> Observable<T> read(final InputStream is) {
        return Observable.create(new AbstractOnSubscribe<T, ObjectInputStream>() {

            @Override
            protected ObjectInputStream onSubscribe(Subscriber<? super T> subscriber) {
                try {
                    return new ObjectInputStream(new BufferedInputStream(is));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void next(SubscriptionState<T, ObjectInputStream> state) {
                ObjectInputStream ois = state.state();
                try {
                    @SuppressWarnings("unchecked")
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

    public static <T extends Serializable> Observable<T> read(final File file) {
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
                return read(is);
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
            }
        };
        return Observable.using(resourceFactory, observableFactory, disposeAction, true);
    }

    public static <T extends Serializable> Observable<T> write(Observable<T> source,
            final OutputStream os) {
        return source.lift(new Operator<T, T>() {

            @Override
            public Subscriber<? super T> call(final Subscriber<? super T> child) {
                try {

                    final ObjectOutputStream ois = new ObjectOutputStream(os);
                    return new Subscriber<T>(child) {

                        @Override
                        public void onCompleted() {
                            child.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            child.onError(e);
                        }

                        @Override
                        public void onNext(T t) {
                            try {
                                ois.writeObject(t);
                            } catch (IOException e) {
                                onError(e);
                                return;
                            }
                            child.onNext(t);
                        }

                    };

                } catch (IOException e) {
                    // ignore everything that the parent does
                    // but ensure gets unsubscribed
                    Subscriber<T> parent = new Subscriber<T>(child) {

                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                        }

                        @Override
                        public void onNext(T t) {
                        }
                    };
                    child.add(parent);
                    child.onError(e);
                    return parent;
                }
            }
        });
    }

    public static <T extends Serializable> Observable<T> write(final Observable<T> source,
            final File file, final boolean append) {
        Func0<OutputStream> resourceFactory = new Func0<OutputStream>() {
            @Override
            public OutputStream call() {
                try {
                    return new BufferedOutputStream(new FileOutputStream(file, append));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Func1<OutputStream, Observable<? extends T>> observableFactory = new Func1<OutputStream, Observable<? extends T>>() {

            @Override
            public Observable<? extends T> call(OutputStream os) {
                return write(source, os);
            }
        };
        Action1<OutputStream> disposeAction = new Action1<OutputStream>() {

            @Override
            public void call(OutputStream os) {
                try {
                    os.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return Observable.using(resourceFactory, observableFactory, disposeAction, true);
    }
}
