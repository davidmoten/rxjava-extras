package com.github.davidmoten.rx;

import rx.Observable;
import rx.functions.Action0;

public  class CloseableObservable<T> {

        private final Observable<T> observable;
        private final Action0 closeAction;

        public CloseableObservable(Observable<T> observable, Action0 closeAction) {
            this.observable = observable;
            this.closeAction = closeAction;
        }

        public Observable<T> observable() {
            return observable;
        }

        public void close() {
            closeAction.call();
        }

    }