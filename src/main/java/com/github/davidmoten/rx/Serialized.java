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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.SyncOnSubscribe;

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
     * @param ois
     *            the {@link ObjectInputStream}
     * @param <T>
     *            the generic type of the returned stream
     * @return the stream of deserialized objects from the {@link InputStream}
     *         as an {@link Observable}.
     */
    public static <T extends Serializable> Observable<T> read(final ObjectInputStream ois) {
        return Observable.create(new SyncOnSubscribe<ObjectInputStream,T>() {

            @Override
            protected ObjectInputStream generateState() {
               return ois;
            }

            @Override
            protected ObjectInputStream next(ObjectInputStream ois, Observer<? super T> observer) {
                try {
                    @SuppressWarnings("unchecked")
                    T t = (T) ois.readObject();
                    observer.onNext(t);
                } catch (EOFException e) {
                    observer.onCompleted();
                } catch (ClassNotFoundException e) {
                    observer.onError(e);
                } catch (IOException e) {
                    observer.onError(e);
                }
                return ois;
            }
        });
    }

    /**
     * Returns the deserialized objects from the given {@link File} as an
     * {@link Observable} stream. Uses buffer of size <code>bufferSize</code>
     * buffer reads from the File.
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
    public static <T extends Serializable> Observable<T> read(final File file,
            final int bufferSize) {
        Func0<ObjectInputStream> resourceFactory = new Func0<ObjectInputStream>() {
            @Override
            public ObjectInputStream call() {
                try {
                    return new ObjectInputStream(
                            new BufferedInputStream(new FileInputStream(file), bufferSize));
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
     * {@link Observable} stream. A buffer size of 8192 bytes is used by
     * default.
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
     * Returns a duplicate of the input stream but with the side effect that
     * emissions from the source are written to the {@link ObjectOutputStream}.
     * 
     * @param source
     *            the source of objects to write
     * @param oos
     *            the output stream to write to
     * @param <T>
     *            the generic type of the objects being serialized
     * @return re-emits the input stream
     */
    public static <T extends Serializable> Observable<T> write(Observable<T> source,
            final ObjectOutputStream oos) {
        return source.doOnNext(new Action1<T>() {

            @Override
            public void call(T t) {
                try {
                    oos.writeObject(t);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * Writes the source stream to the given file in given append mode and using
     * the given buffer size.
     * 
     * @param source
     *            observable stream to write
     * @param file
     *            file to write to
     * @param append
     *            if true writes are appended to file otherwise overwrite the
     *            file
     * @param bufferSize
     *            the buffer size in bytes to use.
     * @param <T>
     *            the generic type of the input stream
     * @return re-emits the input stream
     */
    public static <T extends Serializable> Observable<T> write(final Observable<T> source,
            final File file, final boolean append, final int bufferSize) {
        Func0<ObjectOutputStream> resourceFactory = new Func0<ObjectOutputStream>() {
            @Override
            public ObjectOutputStream call() {
                try {
                    return new ObjectOutputStream(new BufferedOutputStream(
                            new FileOutputStream(file, append), bufferSize));
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

    /**
     * Writes the source stream to the given file in given append mode and using
     * the a buffer size of 8192 bytes.
     * 
     * @param source
     *            observable stream to write
     * @param file
     *            file to write to
     * @param append
     *            if true writes are appended to file otherwise overwrite the
     *            file
     * @param <T>
     *            the generic type of the input stream
     * @return re-emits the input stream
     */
    public static <T extends Serializable> Observable<T> write(final Observable<T> source,
            final File file, final boolean append) {
        return write(source, file, append, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Writes the source stream to the given file in given append mode and using
     * the a buffer size of 8192 bytes.
     * 
     * @param source
     *            observable stream to write
     * @param file
     *            file to write to
     * @param <T>
     *            the generic type of the input stream
     * @return re-emits the input stream
     */
    public static <T extends Serializable> Observable<T> write(final Observable<T> source,
            final File file) {
        return write(source, file, false, DEFAULT_BUFFER_SIZE);
    }

    public static KryoBuilder kryo() {
        return kryo(new Kryo());
    }

    public static KryoBuilder kryo(Kryo kryo) {
        return new KryoBuilder(kryo);
    }

    public static class KryoBuilder {

        private static final int DEFAULT_BUFFER_SIZE = 4096;

        private final Kryo kryo;

        private KryoBuilder(Kryo kryo) {
            this.kryo = kryo;
        }

        public <T> Observable<T> write(final Observable<T> source, final File file) {
            return write(source, file, false, DEFAULT_BUFFER_SIZE);
        }

        public <T> Observable<T> write(final Observable<T> source, final File file,
                boolean append) {
            return write(source, file, append, DEFAULT_BUFFER_SIZE);
        }

        public <T> Observable<T> write(final Observable<T> source, final File file,
                final boolean append, final int bufferSize) {
            Func0<Output> resourceFactory = new Func0<Output>() {
                @Override
                public Output call() {
                    try {
                        return new Output(new FileOutputStream(file, append), bufferSize);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            Func1<Output, Observable<? extends T>> observableFactory = new Func1<Output, Observable<? extends T>>() {

                @Override
                public Observable<? extends T> call(final Output output) {
                    return source.doOnNext(new Action1<T>() {
                        @Override
                        public void call(T t) {
                            kryo.writeObject(output, t);
                        }
                    });
                }
            };
            Action1<Output> disposeAction = new Action1<Output>() {

                @Override
                public void call(Output output) {
                    output.close();
                }
            };
            return Observable.using(resourceFactory, observableFactory, disposeAction, true);
        }

        public <T> Observable<T> read(Class<T> cls, final File file) {
            return read(cls, file, DEFAULT_BUFFER_SIZE);
        }

        public <T> Observable<T> read(final Class<T> cls, final File file, final int bufferSize) {
            Func0<Input> resourceFactory = new Func0<Input>() {
                @Override
                public Input call() {
                    try {
                        return new Input(new FileInputStream(file), bufferSize);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            Func1<Input, Observable<? extends T>> observableFactory = new Func1<Input, Observable<? extends T>>() {

                @Override
                public Observable<? extends T> call(final Input input) {
                    return read(cls, input, bufferSize);
                }
            };
            Action1<Input> disposeAction = new Action1<Input>() {

                @Override
                public void call(Input input) {
                    input.close();
                }
            };
            return Observable.using(resourceFactory, observableFactory, disposeAction, true);
        }

        public <T> Observable<T> read(final Class<T> cls, final Input input, final int bufferSize) {

            return Observable.create(new SyncOnSubscribe<Input,T>() {

                @Override
                protected Input generateState() {
                    return input;
                }

                @Override
                protected Input next(Input arg0, Observer<? super T> observer) {
                    if (input.eof()) {
                        observer.onCompleted();
                    } else {
                        T t = kryo.readObject(input, cls);
                        observer.onNext(t);
                    }
                    return input;
                }
            });
        }
    }

}
