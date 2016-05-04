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
     *             exception
     */
    void serialize(DataOutput output, T t) throws IOException;

    /**
     * Deserializes the bytes pointed by {@code input}.
     * 
     * @param input
     *            input data to read from
     * @return deserialized object
     * @throws IOException
     *             exception
     */
    T deserialize(DataInput input) throws IOException;

    /**
     * Returns the serialized length if constant other wise returns 0 to
     * indicate variable length (which may force more copying in memory and be a
     * bit slower).
     * 
     * @return serialized length or 0 if variable
     */
    int size();
}
