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
import java.io.Serializable;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.AbstractOnSubscribe;

/**
 * Utility class for writing Observable streams to ObjectOutputStreams and
 * reading Observable streams of indeterminate size from ObjectInputStreams.
 */
public final class Serialized {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Returns the deserialized objects from the given {@link InputStream} as an
     * {@link Observable} stream.
     * 
     * @param is
     *            the input stream
     * @param <T>
     *            the generic type of the returned stream
     * @return the stream of deserialized objects from the {@link InputStream}
     *         as an {@link Observable}.
     */
    public static <T extends Serializable> Observable<T> read(final ObjectInputStream ois) {
        return Observable.create(new AbstractOnSubscribe<T, ObjectInputStream>() {

            @Override
            protected ObjectInputStream onSubscribe(Subscriber<? super T> subscriber) {
                return ois;
            }

            @Override
            protected void next(SubscriptionState<T, ObjectInputStream> state) {
                ObjectInputStream ois = state.state();
                try {
                    @SuppressWarnings("unchecked")
                    T t = (T) ois.readObject();
                    state.onNext(t);

                } catch (EOFException e) {
                    state.onCompleted();
                } catch (ClassNotFoundException e) {
                    state.onError(e);
                    return;
                } catch (IOException e) {
                    state.onError(e);
                    return;
                }
            }
        });
    }

    /**
     * Returns the deserialized objects from the given {@link File} as an
     * {@link Observable} stream. Uses buffer of size <code>bufferSize</code> buffer reads from the File.
     * 
     * @param file
     *            the input file
     * @param bufferSize
     *            the buffer size for reading bytes from the file.
     * @param <T>
     *            the generic type of the deserialized objects returned in the
     *            stream
     * @return the stream of deserialized objects from the {@link InputStream}
     *         as an {@link Observable}.
     */
    public static <T extends Serializable> Observable<T> read(final File file, final int bufferSize) {
        Func0<ObjectInputStream> resourceFactory = new Func0<ObjectInputStream>() {
            @Override
            public ObjectInputStream call() {
                try {
                    return new ObjectInputStream(new BufferedInputStream(new FileInputStream(file),
                            bufferSize));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Func1<ObjectInputStream, Observable<? extends T>> observableFactory = new Func1<ObjectInputStream, Observable<? extends T>>() {

            @Override
            public Observable<? extends T> call(ObjectInputStream is) {
                return read(is);
            }
        };
        Action1<ObjectInputStream> disposeAction = new Action1<ObjectInputStream>() {

            @Override
            public void call(ObjectInputStream ois) {
                try {
                    ois.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return Observable.using(resourceFactory, observableFactory, disposeAction, true);
    }

    /**
     * Returns the deserialized objects from the given {@link File} as an
     * {@link Observable} stream. A buffer size of 8192 is used by default.
     * 
     * @param file
     *            the input file containing serialized java objects
     * @param <T>
     *            the generic type of the deserialized objects returned in the
     *            stream
     * @return the stream of deserialized objects from the {@link InputStream}
     *         as an {@link Observable}.
     */
    public static <T extends Serializable> Observable<T> read(final File file) {
        return read(file, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Returns duplicate of the input stream but with the side effect that
     * emissions from the source are written to the input stream wrapped as an
     * ObjectOutputStream. If the output stream is already an ObjectOutputStream
     * then the stream is not wrapped again.
     * 
     * @param source
     * @param os
     * @return
     */
    public static <T extends Serializable> Observable<T> write(Observable<T> source,
            final ObjectOutputStream ois) {
        return source.doOnNext(new Action1<T>() {

            @Override
            public void call(T t) {
                try {
                    ois.writeObject(t);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static <T extends Serializable> Observable<T> write(final Observable<T> source,
            final File file, final boolean append, final int bufferSize) {
        Func0<ObjectOutputStream> resourceFactory = new Func0<ObjectOutputStream>() {
            @Override
            public ObjectOutputStream call() {
                try {
                    return new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                            file, append), bufferSize));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Func1<ObjectOutputStream, Observable<? extends T>> observableFactory = new Func1<ObjectOutputStream, Observable<? extends T>>() {

            @Override
            public Observable<? extends T> call(ObjectOutputStream oos) {
                return write(source, oos);
            }
        };
        Action1<ObjectOutputStream> disposeAction = new Action1<ObjectOutputStream>() {

            @Override
            public void call(ObjectOutputStream oos) {
                try {
                    oos.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return Observable.using(resourceFactory, observableFactory, disposeAction, true);
    }

    public static <T extends Serializable> Observable<T> write(final Observable<T> source,
            final File file, final boolean append) {
        return write(source, file, append, DEFAULT_BUFFER_SIZE);
    }
}
