package com.github.davidmoten.rx.buffertofile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface DataSerializer<T> {

    /**
     * Serializes an object to a data stream.
     * 
     * @param output
     *            the data stream
     * @param t
     *            the object to serialize
     * @throws IOException
     */
    void serialize(DataOutput output, T t) throws IOException;

    /**
     * Deserializes the bytes pointed by {@code input}.
     * 
     * @param input
     *            input data to read from
     * @param availableBytes
     *            the number of bytes available. No bytes available may be
     *            represented by -1 or 0
     * @return deserialized object
     * @throws IOException
     */
    T deserialize(DataInput input, int availableBytes) throws IOException;
}
