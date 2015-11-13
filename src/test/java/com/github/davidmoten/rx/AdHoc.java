package com.github.davidmoten.rx;

import rx.Observable;

public class AdHoc {
    public static void main(String[] args) {
        Observable.range(0, Integer.MAX_VALUE).subscribe();
    }
}
