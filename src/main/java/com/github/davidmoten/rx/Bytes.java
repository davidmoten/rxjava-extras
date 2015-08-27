package com.github.davidmoten.rx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.github.davidmoten.rx.internal.operators.MyZipEntry;
import com.github.davidmoten.rx.internal.operators.OnSubscribeInputStream;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.AbstractOnSubscribe;

public final class Bytes {

    public static Observable<byte[]> from(InputStream is, int size) {
        return Observable.create(new OnSubscribeInputStream(is, size));
    }

    public static Observable<byte[]> from(InputStream is) {
        return from(is, 8192);
    }

    public static Observable<MyZipEntry> unzip(final File file) {
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
        Func1<ZipInputStream, Observable<MyZipEntry>> observableFactory = new Func1<ZipInputStream, Observable<MyZipEntry>>() {
            @Override
            public Observable<MyZipEntry> call(ZipInputStream zis) {
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

    public static Observable<MyZipEntry> unzip(final InputStream is) {
        return unzip(new ZipInputStream(is));
    }

    public static Observable<MyZipEntry> unzip(final ZipInputStream zis) {
        return Observable.create(new AbstractOnSubscribe<MyZipEntry, ZipInputStream>() {
            @Override
            protected void next(
                    AbstractOnSubscribe.SubscriptionState<MyZipEntry, ZipInputStream> state) {
                try {
                    ZipEntry zipEntry = zis.getNextEntry();
                    if (zipEntry != null) {
                        state.onNext(new MyZipEntry(zipEntry, zis));
                    } else {
                        zis.close();
                        state.onCompleted();
                    }
                } catch (IOException e) {
                    Observable.error(e);
                    return;
                }
            }
        });
    }

}
