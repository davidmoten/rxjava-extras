package com.github.davidmoten.rx;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Callable;

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
                    RxJavaHooks.onError(e);
                }
            }
        };
    }

}