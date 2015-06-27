package com.github.davidmoten.rx.operators;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import rx.Notification;
import rx.Notification.Kind;
import rx.Observable;
import rx.Observer;
import rx.functions.Func0;
import rx.functions.Func3;

import com.github.davidmoten.rx.Transformers;

public class TransformerWithStateTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testBufferThree() {
        final int bufferSize = 3;
        Func0<Buffer> initialState = new Func0<Buffer>() {

            @Override
            public Buffer call() {
                return new Buffer();
            }
        };

        Func3<Buffer, Notification<Object>, Observer<List<Object>>, Buffer> transition = new Func3<Buffer, Notification<Object>, Observer<List<Object>>, Buffer>() {

            @Override
            public Buffer call(Buffer buffer, Notification<Object> n,
                    Observer<List<Object>> observer) {
                List<Object> list = new ArrayList<Object>(buffer.list);
                if (n.hasValue()) {
                    list.add(n.getValue());
                    if (list.size() == bufferSize) {
                        observer.onNext(list);
                        return new Buffer();
                    } else {
                        return new Buffer(list);
                    }
                } else {
                    //do error check first because should cut ahead
                    if (n.getKind()== Kind.OnError) {
                        observer.onError(n.getThrowable());
                    } else {
                    if (buffer.list.size() > 0)
                        observer.onNext(buffer.list);
                        observer.onCompleted();
                    }
                    return null;
                }

            }
        };
        List<List<Object>> list = Observable.just(1, 2, 3, 4, 5)
                .compose(Transformers.withState(initialState, transition)).toList().toBlocking()
                .single();
        System.out.println(list);
        assertEquals(asList(asList(1,2,3), asList(4,5)), list);
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
