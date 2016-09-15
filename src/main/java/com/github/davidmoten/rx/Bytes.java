package com.github.davidmoten.rx;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.github.davidmoten.rx.internal.operators.OnSubscribeInputStream;
import com.github.davidmoten.rx.util.ZippedEntry;

import rx.Observable;
import rx.Observable.Transformer;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.SyncOnSubscribe;

public final class Bytes {

    private Bytes() {
        // prevent instantiation
    }

    /**
     * Returns an Observable stream of byte arrays from the given
     * {@link InputStream} between 1 and {@code size} bytes.
     * 
     * @param is
     *            input stream of bytes
     * @param size
     *            max emitted byte array size
     * @return a stream of byte arrays
     */
    public static Observable<byte[]> from(InputStream is, int size) {
        return Observable.create(new OnSubscribeInputStream(is, size));
    }

    public static Observable<byte[]> from(File file) {
        return from(file, 8192);
    }

    public static Observable<byte[]> from(final File file, final int size) {
        Func0<InputStream> resourceFactory = new Func0<InputStream>() {

            @Override
            public InputStream call() {
                try {
                    return new BufferedInputStream(new FileInputStream(file), size);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Func1<InputStream, Observable<byte[]>> observableFactory = new Func1<InputStream, Observable<byte[]>>() {

            @Override
            public Observable<byte[]> call(InputStream is) {
                return from(is, size);
            }
        };
        return Observable.using(resourceFactory, observableFactory, InputStreamCloseHolder.INSTANCE, true);
    }
    
    private static class InputStreamCloseHolder {
        private static final Action1<InputStream> INSTANCE = new Action1<InputStream>() {

            @Override
            public void call(InputStream is) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Returns an Observable stream of byte arrays from the given
     * {@link InputStream} of {@code 8192} bytes. The final byte array may be
     * less than {@code 8192} bytes.
     * 
     * @param is
     *            input stream of bytes
     * @return a stream of byte arrays
     */
    public static Observable<byte[]> from(InputStream is) {
        return from(is, 8192);
    }

    public static Observable<ZippedEntry> unzip(final File file) {
        Func0<ZipInputStream> resourceFactory = new Func0<ZipInputStream>() {
            @Override
            public ZipInputStream call() {
                try {
                    return new ZipInputStream(new FileInputStream(file));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Func1<ZipInputStream, Observable<ZippedEntry>> observableFactory = ZipHolder.OBSERVABLE_FACTORY;
        Action1<ZipInputStream> disposeAction = ZipHolder.DISPOSER;
        return Observable.using(resourceFactory, observableFactory, disposeAction);
    }

    private static final class ZipHolder {
        static final Action1<ZipInputStream> DISPOSER = new Action1<ZipInputStream>() {

            @Override
            public void call(ZipInputStream zis) {
                try {
                    zis.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        final static Func1<ZipInputStream, Observable<ZippedEntry>> OBSERVABLE_FACTORY = new Func1<ZipInputStream, Observable<ZippedEntry>>() {
            @Override
            public Observable<ZippedEntry> call(ZipInputStream zis) {
                return unzip(zis);
            }
        };
    }

    public static Observable<ZippedEntry> unzip(final InputStream is) {
        return unzip(new ZipInputStream(is));
    }

    public static Observable<ZippedEntry> unzip(final ZipInputStream zis) {
        return Observable.create(new SyncOnSubscribe<ZipInputStream, ZippedEntry>() {

            @Override
            protected ZipInputStream generateState() {
                return zis;
            }

            @Override
            protected ZipInputStream next(ZipInputStream zis, Observer<? super ZippedEntry> observer) {
                try {
                    ZipEntry zipEntry = zis.getNextEntry();
                    if (zipEntry != null) {
                        observer.onNext(new ZippedEntry(zipEntry, zis));
                    } else {
                        zis.close();
                        observer.onCompleted();
                    }
                } catch (IOException e) {
                    observer.onError(e);
                }
                return zis;
            }
        });
    }

    public static Transformer<byte[], byte[]> collect() {
        return new Transformer<byte[], byte[]>() {

            @Override
            public Observable<byte[]> call(Observable<byte[]> source) {
                return source.collect(BosCreatorHolder.INSTANCE, BosCollectorHolder.INSTANCE)
                        .map(BosToArrayHolder.INSTANCE);
            }
        };
    }

    private static final class BosCreatorHolder {

        static final Func0<ByteArrayOutputStream> INSTANCE = new Func0<ByteArrayOutputStream>() {

            @Override
            public ByteArrayOutputStream call() {
                return new ByteArrayOutputStream();
            }
        };
    }

    private static final class BosCollectorHolder {

        static final Action2<ByteArrayOutputStream, byte[]> INSTANCE = new Action2<ByteArrayOutputStream, byte[]>() {

            @Override
            public void call(ByteArrayOutputStream bos, byte[] bytes) {
                try {
                    bos.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private static final class BosToArrayHolder {
        static final Func1<ByteArrayOutputStream, byte[]> INSTANCE = new Func1<ByteArrayOutputStream, byte[]>() {
            @Override
            public byte[] call(ByteArrayOutputStream bos) {
                return bos.toByteArray();
            }
        };
    }

}
