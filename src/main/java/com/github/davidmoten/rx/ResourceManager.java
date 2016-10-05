package com.github.davidmoten.rx;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import com.github.davidmoten.rx.exceptions.IORuntimeException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.plugins.RxJavaHooks;

//TODO as suggested by George Campbell, may replace with his PR
public class ResourceManager<T> {

    private final Func0<T> resourceFactory;
    private final Action1<? super T> disposeAction;

    private ResourceManager(Func0<T> resourceFactory, Action1<? super T> disposeAction) {
        this.resourceFactory = resourceFactory;
        this.disposeAction = disposeAction;
    }

    public static <T> ResourceManager<T> create(Func0<T> resourceFactory,
            Action1<? super T> disposeAction) {
        return new ResourceManager<T>(resourceFactory, disposeAction);
    }

    public static <T> ResourceManager<T> create(Callable<T> resourceFactory,
            Action1<? super T> disposeAction) {
        return new ResourceManager<T>(Functions.toFunc0(resourceFactory), disposeAction);
    }

    public static <T> ResourceManager<T> create(Callable<T> resourceFactory,
            Checked.A1<? super T> disposeAction) {
        return new ResourceManager<T>(Functions.toFunc0(resourceFactory),
                Checked.a1(disposeAction));
    }

    public static <T extends Closeable> ResourceManager<T> create(Func0<T> resourceFactory) {
        return create(resourceFactory, CloserHolder.INSTANCE);
    }

    public static ResourceManager<OutputStream> forWriting(final File file) {
        Func0<OutputStream> rf = new Func0<OutputStream>() {

            @Override
            public OutputStream call() {
                try {
                    return new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    throw new IORuntimeException(e);
                }
            }
        };
        return create(rf);
    }

    public static ResourceManager<InputStream> forReading(final File file) {
        Func0<InputStream> rf = new Func0<InputStream>() {

            @Override
            public InputStream call() {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    throw new IORuntimeException(e);
                }
            }
        };
        return create(rf);
    }

    public static ResourceManager<InputStream> forReadingFromClasspath(final Class<?> cls,
            final String resource) {
        Func0<InputStream> rf = new Func0<InputStream>() {
            @Override
            public InputStream call() {
                return cls.getResourceAsStream(resource);
            }
        };
        return create(rf);
    }

    public Observable<T> observable(Func1<? super T, Observable<? extends T>> observableFactory,
            boolean disposeEagerly) {
        return Observable.using(resourceFactory, observableFactory, disposeAction, disposeEagerly);
    }

    public Observable<T> observable(Func1<? super T, Observable<? extends T>> observableFactory) {
        return observable(observableFactory, false);
    }
    
    private static final class CloserHolder {

        static final Action1<Closeable> INSTANCE = new Action1<Closeable>() {

            @Override
            public void call(Closeable c) {
                try {
                    c.close();
                } catch (IOException e) {
                    //TODO ignore or send to RxJavaHooks?
                    RxJavaHooks.onError(e);
                }
            }
        };
    }

}
