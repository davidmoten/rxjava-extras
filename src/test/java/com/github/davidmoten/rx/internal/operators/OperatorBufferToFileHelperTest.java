package com.github.davidmoten.rx.internal.operators;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.buffertofile.DataSerializer;
import com.github.davidmoten.rx.testing.TestingHelper;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

public class OperatorBufferToFileHelperTest extends TestCase {

    public static TestSuite suite() {
        return TestingHelper
                // sync
                .function(BUFFER)
                //
                .name("testEmpty").fromEmpty().expectEmpty()
                //
                .name("testOne").from(1).expect(1)
                //
                .name("testTwo").from(1, 2).expect(1, 2)
                //
                .name("testError").fromError().expectError()
                //
                // get test suites
                .testSuite(OperatorBufferToFileHelperTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }

    private static final Func1<Observable<Integer>, Observable<Integer>> BUFFER = new Func1<Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<Integer> o) {
            return o.compose(Transformers.onBackpressureBufferToFile(serializer(),
                    rx.schedulers.Schedulers.computation()));
        }

    };

    private static DataSerializer<Integer> serializer() {
        return new DataSerializer<Integer>() {

            @Override
            public void serialize(DataOutput output, Integer t) throws IOException {
                output.writeInt(t);
            }

            @Override
            public Integer deserialize(DataInput input, int availableBytes) throws IOException {
                return input.readInt();
            }
        };
    }
}