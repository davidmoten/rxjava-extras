package com.github.davidmoten.rx;

import java.io.BufferedInputStream;
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
import rx.Observer;
import rx.functions.Action1;
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
        Func1<ZipInputStream, Observable<ZippedEntry>> observableFactory = new Func1<ZipInputStream, Observable<ZippedEntry>>() {
            @Override
            public Observable<ZippedEntry> call(ZipInputStream zis) {
                return unzip(zis);
            }
        };
        Action1<ZipInputStream> disposeAction = new Action1<ZipInputStream>() {

            @Override
            public void call(ZipInputStream zis) {
                try {
                    zis.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return Observable.using(resourceFactory, observableFactory, disposeAction);
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
            protected ZipInputStream next(ZipInputStream zis,
                    Observer<? super ZippedEntry> observer) {
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

}
