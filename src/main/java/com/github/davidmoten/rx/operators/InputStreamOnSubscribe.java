package com.github.davidmoten.rx.operators;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.github.davidmoten.rx.util.Optional;

public class InputStreamOnSubscribe extends AbstractOnSubscribe<byte[]> {

    private final int size;
    private final InputStream is;

    public InputStreamOnSubscribe(InputStream is, int size) {
        this.is = is;
        this.size = size;
    }

    @Override
    public Optional<byte[]> next() {
        try {
            byte[] bytes = new byte[size];
            int n = is.read(bytes);
            if (n == -1)
                return Optional.absent();
            else
                return Optional.of(Arrays.copyOf(bytes, n));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
