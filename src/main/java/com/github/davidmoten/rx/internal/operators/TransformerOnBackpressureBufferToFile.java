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

    private final File file;
    private final Serializer<T> serializer;

    public TransformerOnBackpressureBufferToFile(File file, Serializer<T> serializer) {
        this.file = file;
        this.serializer = serializer;
    }

    @Override
    public Observable<T> call(Observable<T> o) {
        throw new RuntimeException("not implemented yet");
    }

    public static void main(String[] args) {
        System.out.println(1 | 1);
    }

}
