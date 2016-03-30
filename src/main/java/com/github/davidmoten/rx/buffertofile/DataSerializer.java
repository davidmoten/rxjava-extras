package com.github.davidmoten.rx.buffertofile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public  interface DataSerializer<T> {
        void serialize(DataOutput output, T t) throws IOException;

        T deserialize(DataInput input, int size) throws IOException;
    }
