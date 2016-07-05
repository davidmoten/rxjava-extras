package com.github.davidmoten.rx.internal.operators;

import rx.*;
import rx.Observable.*;
import rx.exceptions.*;
import rx.functions.Func1;
import rx.internal.producers.ProducerArbiter;

/**
 * Switches to different Observables if the main source completes or signals an error.
 *
 * @param <T> the value type
 */
public final class TransformerOnTerminateResume<T> implements Transformer<T, T> {
    
    final Func1<Throwable, Observable<T>> onError;
    
    final Observable<T> onCompleted;

    public TransformerOnTerminateResume(Func1<Throwable, Observable<T>> onError, Observable<T> onCompleted) {
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    @Override
    public Observable<T> call(final Observable<T> o) {
        return Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> t) {
                OnTerminateResumeSubscriber<T> parent = new OnTerminateResumeSubscriber<T>(t, onError, onCompleted);
                
                t.add(parent);
                t.setProducer(parent.arbiter);
                
                o.unsafeSubscribe(parent);
            }
        });
    }
    
    static final class OnTerminateResumeSubscriber<T> extends Subscriber<T> {

        final Subscriber<? super T> actual;
        
        final Func1<Throwable, Observable<T>> onError;
        
        final Observable<T> onCompleted;

        final ProducerArbiter arbiter;
        
        long produced;
        
        public OnTerminateResumeSubscriber(Subscriber<? super T> actual, Func1<Throwable, 
                Observable<T>> onError,
                Observable<T> onCompleted) {
            this.arbiter = new ProducerArbiter();
            this.actual = actual;
            this.onError = onError;
            this.onCompleted = onCompleted;
        }

        @Override
        public void onNext(T t) {
            produced++;
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            long p = produced;
            if (p != 0L) {
                arbiter.produced(p);
            }
            
            Observable<T> o;
            
            try {
                o = onError.call(e);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                
                actual.onError(new CompositeException(e, ex));
                
                return;
            }
            
            if (o == null) {
                actual.onError(new NullPointerException("The onError function returned a null Observable."));
            } else {
                o.unsafeSubscribe(new ResumeSubscriber<T>(actual, arbiter));
            }
        }

        @Override
        public void onCompleted() {
            long p = produced;
            if (p != 0L) {
                arbiter.produced(p);
            }
            onCompleted.unsafeSubscribe(new ResumeSubscriber<T>(actual, arbiter));
        }
        
        @Override
        public void setProducer(Producer p) {
            arbiter.setProducer(p);
        }
        
        static final class ResumeSubscriber<T> extends Subscriber<T> {
            final Subscriber<? super T> actual;
            
            final ProducerArbiter arbiter;

            public ResumeSubscriber(Subscriber<? super T> actual, ProducerArbiter arbiter) {
                this.actual = actual;
                this.arbiter = arbiter;
            }

            @Override
            public void onCompleted() {
                actual.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                actual.onError(e);
            }

            @Override
            public void onNext(T t) {
                actual.onNext(t);
            }

            @Override
            public void setProducer(Producer p) {
                arbiter.setProducer(p);
            }
        }
    }
}
