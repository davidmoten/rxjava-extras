package com.github.davidmoten.rx.internal.operators;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Func1;
import rx.observables.AbstractOnSubscribe;

public class TransformerZip implements Transformer<InputStream, EntryBytes> {

    @Override
    public Observable<EntryBytes> call(Observable<InputStream> o) {
        return o.flatMap(new Func1<InputStream, Observable<EntryBytes>>() {

            @Override
            public Observable<EntryBytes> call(final InputStream is) {
                return Observable.create(new AbstractOnSubscribe<EntryBytes, ZipInputStream>() {

                    ZipInputStream zis = new ZipInputStream(is);
                    ZipEntry zipEntry = null;

                    @Override
                    protected void next(
                            AbstractOnSubscribe.SubscriptionState<EntryBytes, ZipInputStream> state) {
                        try {
                            if (zipEntry == null) {
                                zipEntry = zis.getNextEntry();
                            }
                            if (zipEntry != null) {
                                byte[] bytes = new byte[(int) zipEntry.getSize()];
                                zis.read(bytes);
                                state.onNext(new EntryBytes(new Entry(zipEntry), bytes));
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
        });
    }
}
