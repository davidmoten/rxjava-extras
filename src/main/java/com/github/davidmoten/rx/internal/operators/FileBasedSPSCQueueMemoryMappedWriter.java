package com.github.davidmoten.rx.internal.operators;

import static com.github.davidmoten.rx.internal.operators.FileBasedSPSCQueueMemoryMapped.EOF_MARKER;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.util.ByteArrayOutputStreamNoCopyUnsynchronized;

public final class FileBasedSPSCQueueMemoryMappedWriter<T> {

    private final RandomAccessFile f;
    private final DataOutputStream output;
    private final MappedByteBuffer write;
    private final DataSerializer<T> serializer;
    private final DataOutput buffer;
    private final ByteArrayOutputStreamNoCopyUnsynchronized bytes;

    public FileBasedSPSCQueueMemoryMappedWriter(File file, int fileSize,
            DataSerializer<T> serializer) {
        this.serializer = serializer;
        try {
            f = new RandomAccessFile(file, "rw");
            write = f.getChannel().map(MapMode.READ_WRITE, 0, fileSize);
            write.putInt(0);
            output = new DataOutputStream(new MappedByteBufferOutputStream(write));
            bytes = new ByteArrayOutputStreamNoCopyUnsynchronized();
            buffer = new DataOutputStream(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class MappedByteBufferOutputStream extends OutputStream {

        private final MappedByteBuffer write;

        MappedByteBufferOutputStream(MappedByteBuffer write) {
            this.write = write;
        }

        @Override
        public void write(int b) throws IOException {
            write.put((byte) b);
        }
    }

    public boolean offer(T t) {
        // the current position will be just past the length bytes for this
        // item (length bytes will be 0 at the moment)
        int serializedLength = serializer.size();
        if (serializedLength == 0) {
            try {
                bytes.reset();
                // serialize to an in-memory buffer to calculate length
                serializer.serialize(buffer, t);
                if (bytes.size() + 4 > write.remaining()) {
                    write.putInt(EOF_MARKER);
                    close();
                    return false;
                } else {
                    write.put(bytes.toByteArrayNoCopy(), 0, bytes.size());
                    write.putInt(0);
                    // remember the position
                    int newWritePosition = write.position();
                    // rewind and update the length for the current item
                    write.position(write.position() - bytes.size() - 8);
                    // now indicate to the reader that it can read this item
                    output.writeInt(bytes.size());
                    // and update the position to the write position for the
                    // next item
                    write.position(newWritePosition);
                    return true;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (serializedLength + 4 > write.remaining()) {
            write.putInt(EOF_MARKER);
            close();
            return false;
        } else {
            int position = write.position();
            try {
                // serialize the object t to the file
                serializer.serialize(output, t);
                int length = write.position() - position;
                if (length > serializedLength) {
                    throw new IllegalArgumentException(
                            "serialized length of t was greater than serializedLength");
                }
                // write a length of zero for the next item
                write.putInt(0);
                // remember the position
                int newWritePosition = write.position();
                // rewind and update the length for the current item
                write.position(position - 4);
                // now indicate to the reader that it can read this item
                // because the length will now be non-zero
                output.writeInt(length);
                // and update the position to the write position for the next
                // item
                write.position(newWritePosition);
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void close() {
        write.force();
        try {
            f.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
