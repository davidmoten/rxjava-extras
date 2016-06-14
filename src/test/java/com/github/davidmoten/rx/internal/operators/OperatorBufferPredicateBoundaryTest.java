package com.github.davidmoten.rx.internal.operators;

import java.io.IOException;
import java.util.*;

import org.junit.*;

import com.github.davidmoten.rx.Transformers;

import rx.Observable;
import rx.functions.Func1;
import rx.internal.util.*;
import rx.observers.TestSubscriber;

public class OperatorBufferPredicateBoundaryTest {

    @Test
    public void bufferWhilePredicateNull() {
        try {
            Observable.empty().compose(Transformers.bufferWhile(null));
            Assert.fail("Didn't throw NullPointerException");
        } catch (NullPointerException ex) {
            Assert.assertEquals("predicate", ex.getMessage());
        }
    }

    @Test
    public void bufferUntilPredicateNull() {
        try {
            Observable.empty().compose(Transformers.bufferUntil(null));
            Assert.fail("Didn't throw NullPointerException");
        } catch (NullPointerException ex) {
            Assert.assertEquals("predicate", ex.getMessage());
        }
    }

    @Test
    public void bufferWhileCapacityHintInvalid() {
        try {
            Observable.empty().compose(Transformers.bufferWhile(UtilityFunctions.alwaysTrue(), -99));
            Assert.fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("capacityHint > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void bufferUntilCapacityHintInvalid() {
        try {
            Observable.empty().compose(Transformers.bufferUntil(UtilityFunctions.alwaysTrue(), -99));
            Assert.fail("Didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("capacityHint > 0 required but it was -99", ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferWhileWith1() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        
        Observable.range(1, 10)
        .compose(Transformers.bufferWhile(UtilityFunctions.<Integer>alwaysTrue()))
        .subscribe(ts);
        
        ts.assertValues(
                Arrays.asList(1),
                Arrays.asList(2),
                Arrays.asList(3),
                Arrays.asList(4),
                Arrays.asList(5),
                Arrays.asList(6),
                Arrays.asList(7),
                Arrays.asList(8),
                Arrays.asList(9),
                Arrays.asList(10)
            );
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferWhileWith5() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        
        Observable.range(1, 10)
        .compose(Transformers.bufferWhile(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) {
                return v == 5;
            }
        }))
        .subscribe(ts);
        
        ts.assertValues(
                Arrays.asList(1, 2, 3, 4),
                Arrays.asList(5, 6, 7, 8, 9, 10)
            );
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferWhileWith20() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        
        Observable.range(1, 10)
        .compose(Transformers.bufferWhile(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) {
                return v == 20;
            }
        }))
        .subscribe(ts);
        
        ts.assertValues(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            );
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferUntilWith1() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        
        Observable.range(1, 10)
        .compose(Transformers.bufferUntil(UtilityFunctions.<Integer>alwaysTrue()))
        .subscribe(ts);
        
        ts.assertValues(
                Arrays.asList(1),
                Arrays.asList(2),
                Arrays.asList(3),
                Arrays.asList(4),
                Arrays.asList(5),
                Arrays.asList(6),
                Arrays.asList(7),
                Arrays.asList(8),
                Arrays.asList(9),
                Arrays.asList(10)
            );
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferUntilWith5() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        
        Observable.range(1, 10)
        .compose(Transformers.bufferUntil(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) {
                return v == 5;
            }
        }))
        .subscribe(ts);
        
        ts.assertValues(
                Arrays.asList(1, 2, 3, 4, 5),
                Arrays.asList(6, 7, 8, 9, 10)
            );
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferUntilWith20() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        
        Observable.range(1, 10)
        .compose(Transformers.bufferUntil(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) {
                return v == 20;
            }
        }))
        .subscribe(ts);
        
        ts.assertValues(
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            );
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferWhileSomeNull() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        
        Observable.just(1, 2, 3, null, 4, 5)
        .compose(Transformers.bufferUntil(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) {
                return v != null && v == 20;
            }
        }))
        .subscribe(ts);
        
        ts.assertValues(
                Arrays.asList(1, 2, 3, null, 4, 5)
            );
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferUntilSomeNull() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        
        Observable.just(1, 2, 3, null, 4, 5)
        .compose(Transformers.bufferUntil(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) {
                return v != null && v == 20;
            }
        }))
        .subscribe(ts);
        
        ts.assertValues(
                Arrays.asList(1, 2, 3, null, 4, 5)
            );
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void bufferWhilePredicateThrows() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        
        Observable.just(1, 2, 3, null, 4, 5)
        .compose(Transformers.bufferWhile(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) {
                return v == 20;
            }
        }))
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(NullPointerException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void bufferUntilPredicateThrows() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();
        
        Observable.just(1, 2, 3, null, 4, 5)
        .compose(Transformers.bufferUntil(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) {
                return v == 20;
            }
        }))
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(NullPointerException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void emptySourceBufferWhile() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        Observable.<Integer>empty()
        .compose(Transformers.bufferWhile(UtilityFunctions.<Integer>alwaysTrue()))
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void emptySourceBufferUntil() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        Observable.<Integer>empty()
        .compose(Transformers.bufferUntil(UtilityFunctions.<Integer>alwaysTrue()))
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void errorSourceBufferWhile() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        Observable.<Integer>error(new IOException())
        .compose(Transformers.bufferWhile(UtilityFunctions.<Integer>alwaysTrue()))
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(IOException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void errorSourceBufferUntil() {
        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        Observable.<Integer>error(new IOException())
        .compose(Transformers.bufferUntil(UtilityFunctions.<Integer>alwaysTrue()))
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(IOException.class);
        ts.assertNotCompleted();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void bufferWhileBackpressure() {
        TestSubscriber<List<Integer>> ts = TestSubscriber.create(0);
        
        Observable.range(1, RxRingBuffer.SIZE * 4)
        .compose(Transformers.bufferWhile(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) {
                return (v % 3) == 0;
            }
        })).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        ts.assertValue(Arrays.asList(1, 2));

        ts.requestMore(1);
        ts.assertValues(
                Arrays.asList(1, 2),
                Arrays.asList(3, 4, 5)
        );
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        ts.assertValues(
                Arrays.asList(1, 2),
                Arrays.asList(3, 4, 5),
                Arrays.asList(6, 7, 8)
        );
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(Long.MAX_VALUE);
        
        ts.assertValueCount((RxRingBuffer.SIZE * 4) / 3 + 1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bufferUntilBackpressure() {
        TestSubscriber<List<Integer>> ts = TestSubscriber.create(0);
        
        Observable.range(1, RxRingBuffer.SIZE * 4)
        .compose(Transformers.bufferUntil(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) {
                return (v % 3) == 0;
            }
        })).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        ts.assertValue(Arrays.asList(1, 2, 3));

        ts.requestMore(1);
        ts.assertValues(
                Arrays.asList(1, 2, 3),
                Arrays.asList(4, 5, 6)
        );
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        ts.assertValues(
                Arrays.asList(1, 2, 3),
                Arrays.asList(4, 5, 6),
                Arrays.asList(7, 8, 9)
        );
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(Long.MAX_VALUE);
        
        ts.assertValueCount((RxRingBuffer.SIZE * 4) / 3 + 1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void bufferWhileBackpressureFullBuffer() {
        TestSubscriber<List<Integer>> ts = TestSubscriber.create(0);
        
        int count = RxRingBuffer.SIZE * 4;
        
        Observable.range(1, count)
        .compose(Transformers.bufferWhile(UtilityFunctions.<Integer>alwaysFalse()))
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        ts.assertValueCount(1);
        ts.assertNoErrors();
        ts.assertCompleted();
        
        Assert.assertEquals(count, ts.getOnNextEvents().get(0).size());
    }

    @Test
    public void bufferUntilBackpressureFullBuffer() {
        TestSubscriber<List<Integer>> ts = TestSubscriber.create(0);
        
        int count = RxRingBuffer.SIZE * 4;
        
        Observable.range(1, count)
        .compose(Transformers.bufferUntil(UtilityFunctions.<Integer>alwaysFalse()))
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        ts.assertValueCount(1);
        ts.assertNoErrors();
        ts.assertCompleted();
        
        Assert.assertEquals(count, ts.getOnNextEvents().get(0).size());
    }

}