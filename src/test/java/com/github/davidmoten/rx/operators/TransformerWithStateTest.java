package com.github.davidmoten.rx.operators;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.functions.Func0;
import rx.functions.Func3;

import com.github.davidmoten.rx.Transformers;

public class TransformerWithStateTest {

    @Test
    public void testBufferThree() {
        Func0<Buffer> initialState = new Func0<Buffer>() {

            @Override
            public Buffer call() {
                return new Buffer();
            }
        };

        Func3<Buffer, Object, Observer<Object>, Buffer> transition = new Func3<Buffer, Object, Observer<Object>, Buffer>() {

            @Override
            public Buffer call(Buffer buffer, Object t, Observer<Object> observer) {
                List<Object> list = new ArrayList<Object>(buffer.list);
                list.add(t);
                if (list.size() == 3) {
                    observer.onNext(list);
                    return new Buffer();
                } else {
                    return new Buffer(list);
                }
            }
        };
        Observable.just(1, 2, 3, 4, 5).compose(Transformers.withState(initialState, transition))
                .toList().toBlocking().single();
    }

    private static class Buffer {
        List<Object> list;

        Buffer(List<Object> list) {
            this.list = list;
        }

        Buffer() {
            this(new ArrayList<Object>());
        }

    }
}
