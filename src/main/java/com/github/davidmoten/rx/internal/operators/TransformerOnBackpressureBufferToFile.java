package com.github.davidmoten.rx.internal.operators;

import java.io.File;
import java.util.concurrent.BlockingQueue;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

public final class TransformerOnBackpressureBufferToFile<T> implements Transformer<T, T> {

    private static final String QUEUE_NAME = "q";
    private final File file;
    private final Serializer<T> serializer;

    public TransformerOnBackpressureBufferToFile(File file, Serializer<T> serializer) {
        this.file = file;
        this.serializer = serializer;
    }

    @Override
    public Observable<T> call(Observable<T> o) {
        return Observable.defer(new Func0<Observable<T>>() {

            @Override
            public Observable<T> call() {
                Func0<DB> resourceFactory = new Func0<DB>() {
                    @Override
                    public DB call() {
                        return DBMaker.newFileDB(file).make();
                    }
                };
                Func1<DB, Observable<T>> observableFactory = new Func1<DB, Observable<T>>() {
                    @Override
                    public Observable<T> call(DB db) {
                        final BlockingQueue<T> queue;
                        if (db.getCatalog().containsKey(QUEUE_NAME)) {
                            queue = db.getQueue(QUEUE_NAME);
                        } else {
                            queue = db.createQueue(QUEUE_NAME, serializer, false);
                        }
                        return Observable.create(new OnSubscribeFromQueue<T>(queue));
                    }
                };
                Action1<DB> disposeAction = new Action1<DB>() {
                    @Override
                    public void call(DB db) {
                        db.close();
                    }
                };
                return Observable.using(resourceFactory, observableFactory, disposeAction, true)
                        .repeat();
            }
        });
    }

    public static void main(String[] args) {
        System.out.println(1 | 1);
    }

}
