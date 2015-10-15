package com.github.davidmoten.rx;

import rx.Observable;
import rx.functions.Action0;

public  class CloseableObservableWithReset<T> {

        private final Observable<T> observable;
        private final Action0 closeAction;
        private final Action0 resetAction;

        public CloseableObservableWithReset(Observable<T> observable, Action0 closeAction, Action0 resetAction) {
            this.observable = observable;
            this.closeAction = closeAction;
            this.resetAction = resetAction;
        }

        public Observable<T> observable() {
            return observable;
        }

        public void reset() {
            resetAction.call();
        }
        
        public void close() {
            closeAction.call();
        }

    }