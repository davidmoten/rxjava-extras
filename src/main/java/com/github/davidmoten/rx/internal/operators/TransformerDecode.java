package com.github.davidmoten.rx.internal.operators;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import com.github.davidmoten.rx.Transformers;

import rx.Observable.Transformer;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.functions.Func3;

public final class TransformerDecode {

    public static Transformer<byte[], String> decode(final CharsetDecoder decoder) {
        Func0<ByteBuffer> initialState = new Func0<ByteBuffer>() {

            @Override
            public ByteBuffer call() {
                return null;
            }
        };
        Func3<ByteBuffer, byte[], Subscriber<String>, ByteBuffer> transition = new Func3<ByteBuffer, byte[], Subscriber<String>, ByteBuffer>() {

            @Override
            public ByteBuffer call(ByteBuffer last, byte[] next, Subscriber<String> o) {
                Result result = process(next, last, false, decoder, o);
                return result.leftOver;
            }
        };
        Func2<ByteBuffer, Subscriber<String>, Boolean> completion = new Func2<ByteBuffer, Subscriber<String>, Boolean>() {

            @Override
            public Boolean call(ByteBuffer last, Subscriber<String> subscriber) {
                return process(null, last, true, decoder, subscriber).canEmitFurther;
            }
        };

        return Transformers.stateMachine(initialState, transition, completion);
    }

    private static final class Result {
        final ByteBuffer leftOver;
        final boolean canEmitFurther;

        Result(ByteBuffer leftOver, boolean canEmitFurther) {
            this.leftOver = leftOver;
            this.canEmitFurther = canEmitFurther;
        }

    }

    public static Result process(byte[] next, ByteBuffer last, boolean endOfInput,
            CharsetDecoder decoder, Subscriber<String> o) {
        if (o.isUnsubscribed())
            return new Result(null, false);

        ByteBuffer bb;
        if (last != null) {
            if (next != null) {
                // merge leftover in front of the next bytes
                bb = ByteBuffer.allocate(last.remaining() + next.length);
                bb.put(last);
                bb.put(next);
                bb.flip();
            } else { // next == null
                bb = last;
            }
        } else { // last == null
            if (next != null) {
                bb = ByteBuffer.wrap(next);
            } else { // next == null
                return new Result(null, true);
            }
        }

        CharBuffer cb = CharBuffer.allocate((int) (bb.limit() * decoder.averageCharsPerByte()));
        CoderResult cr = decoder.decode(bb, cb, endOfInput);
        cb.flip();

        if (cr.isError()) {
            try {
                cr.throwException();
            } catch (CharacterCodingException e) {
                o.onError(e);
                return new Result(null, false);
            }
        }

        ByteBuffer leftOver;
        if (bb.remaining() > 0) {
            leftOver = bb;
        } else {
            leftOver = null;
        }

        String string = cb.toString();
        if (!string.isEmpty())
            o.onNext(string);

        return new Result(leftOver, true);
    }

}
