package com.github.davidmoten.rx.subjects;

import java.util.concurrent.atomic.AtomicReference;

import rx.Subscriber;
import rx.subjects.Subject;

/**
 * A {@link Subject} that supports a maximum of one {@link Subscriber}. When
 * there is no subscriber any notifications (<code>onNext</code>,
 * <code>onError</code>, <code>onCompleted</code>) are ignored.
 * 
 * @param <T>
 *            type of items being emitted/observed by this subject
 */
public final class PublishSubjectSingleSubscriber<T> extends Subject<T, T> {

    // Visible for testing
    static final String ONLY_ONE_SUBSCRIPTION_IS_ALLOWED = "only one subscription is allowed";

    private final SingleSubscribeOnSubscribe<T> onSubscribe;

    private PublishSubjectSingleSubscriber(SingleSubscribeOnSubscribe<T> onSubscribe) {
        super(onSubscribe);
        this.onSubscribe = onSubscribe;
    }

    private PublishSubjectSingleSubscriber() {
        this(new SingleSubscribeOnSubscribe<T>());
    }

    /**
     * Returns a new instance of a {@link PublishSubjectSingleSubscriber}.
     * 
     * @return the new instance
     * @param <T>
     *            type of items being emitted/observed by this subject
     */
    public static <T> PublishSubjectSingleSubscriber<T> create() {
        return new PublishSubjectSingleSubscriber<T>();
    }

    @Override
    public void onCompleted() {
    	Subscriber<? super T> sub = onSubscribe.subscriber.get();
        if (sub != null) {
            sub.onCompleted();
        }
    }

    @Override
    public void onError(Throwable e) {
    	Subscriber<? super T> sub = onSubscribe.subscriber.get();
        if (sub != null) {
            sub.onError(e);
        }
    }

    @Override
    public void onNext(T t) {
        Subscriber<? super T> sub = onSubscribe.subscriber.get();
        if (sub != null) {
            sub.onNext(t);
        }
    }

    private static class SingleSubscribeOnSubscribe<T> implements OnSubscribe<T> {

        final AtomicReference<Subscriber<? super T>> subscriber = new AtomicReference<Subscriber<? super T>>();

        @Override
        public void call(Subscriber<? super T> sub) {
            if (!subscriber.compareAndSet(null, sub))
                throw new RuntimeException(ONLY_ONE_SUBSCRIPTION_IS_ALLOWED);
        }

    }

    @Override
    public boolean hasObservers() {
        return onSubscribe.subscriber.get() != null;
    }

}