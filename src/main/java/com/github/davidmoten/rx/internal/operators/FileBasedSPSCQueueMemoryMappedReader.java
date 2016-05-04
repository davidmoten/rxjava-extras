package com.github.davidmoten.rx.internal.operators;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

import com.github.davidmoten.rx.buffertofile.DataSerializer;

public class FileBasedSPSCQueueMemoryMappedReader<T> {

    private final RandomAccessFile f;
    private final DataInputStream input;
    private final MappedByteBuffer read;
    private final DataSerializer<T> serializer;
    private final File file;

    public FileBasedSPSCQueueMemoryMappedReader(File file, int fileSize,
            DataSerializer<T> serializer) {
        this.file = file;
        this.serializer = serializer;
        try {
            f = new RandomAccessFile(file, "r");
            read = f.getChannel().map(MapMode.READ_ONLY, 0, fileSize);
            input = new DataInputStream(new MappedByteBufferInputStream(read));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File file() {
        return file;
    }

    private static class MappedByteBufferInputStream extends InputStream {

        private final MappedByteBuffer read;

        MappedByteBufferInputStream(MappedByteBuffer read) {
            this.read = read;
        }

        @Override
        public int read() throws IOException {
            return read.get();
        }

    }

    private static final EOFRuntimeException EOF = new EOFRuntimeException();

    static final class EOFRuntimeException extends RuntimeException {

        private static final long serialVersionUID = -6943467453336359472L;

    }

    public T poll() {
        read.mark();
        int length = read.getInt();
        if (length == -1) {
            throw EOF;
        } else if (length == 0) {
            read.reset();
            return null;
        } else {
            try {
                T t = serializer.deserialize(input);
                if (t == null) {
                    // this is a trick that we can get away with due to type
                    // erasure in java as long as the return value of poll() is
                    // checked using NullSentinel.isNullSentinel(t) (?)
                    return NullSentinel.instance();
                } else {
                    return t;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void close() {
        try {
            f.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
