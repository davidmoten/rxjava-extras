package com.github.davidmoten.rx;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.github.davidmoten.rx.internal.operators.MyZipEntry;
import com.github.davidmoten.rx.internal.operators.OnSubscribeInputStream;

import rx.Observable;
import rx.observables.AbstractOnSubscribe;

public final class Bytes {

    public static Observable<byte[]> from(InputStream is, int size) {
        return Observable.create(new OnSubscribeInputStream(is, size));
    }

    public static Observable<byte[]> from(InputStream is) {
        return from(is, 8192);
    }

    public static Observable<MyZipEntry> unzip(final InputStream is) {
        return Observable.create(new AbstractOnSubscribe<MyZipEntry, ZipInputStream>() {

            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry zipEntry = null;

            @Override
            protected void next(
                    AbstractOnSubscribe.SubscriptionState<MyZipEntry, ZipInputStream> state) {
                try {
                    if (zipEntry == null) {
                        zipEntry = zis.getNextEntry();
                    }
                    if (zipEntry != null) {
                        ZipEntry ze = zipEntry;
                        zipEntry = null;
                        state.onNext(new MyZipEntry(ze, zis));
                    } else {
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
