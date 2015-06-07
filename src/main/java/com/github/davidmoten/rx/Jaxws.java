package com.github.davidmoten.rx;

import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import rx.Observable;

public class Jaxws {

    @SuppressWarnings("rawtypes")
    public static final class ObservableAdapter extends XmlAdapter<List, Observable> {

        @SuppressWarnings({ "unchecked" })
        @Override
        public Observable unmarshal(List list) throws Exception {
            return Observable.from(list);
        }

        @Override
        public List marshal(Observable o) throws Exception {
            return (List) o.toList().toBlocking().single();
        }

    }

}
