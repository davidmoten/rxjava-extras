rxjava-extras
=============

<a href="https://travis-ci.org/davidmoten/rxjava-extras"><img src="https://travis-ci.org/davidmoten/rxjava-extras.svg"/></a><br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/rxjava-extras/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/rxjava-extras)<br/>
[![Dependency Status](https://gemnasium.com/com.github.davidmoten/rxjava-extras.svg)](https://gemnasium.com/com.github.davidmoten/rxjava-extras)


Utilities for use with rxjava (some were struck out of RxJava core for 1.0.0):

* ```Functions.identity```
* ```Functions.alwaysTrue```
* ```Functions.alwaysFalse```
* ```Functions.constant```
* ```Functions.not```
* ```TestingHelper```
* ```OperatorUnsubscribeEagerly```
* ```Checked``` provides lambda helpers for dealing with checked exceptions in functions and actions


Status: *released to Maven Central*

Maven site reports are [here](http://davidmoten.github.io/rxjava-extras/index.html) including [javadoc](http://davidmoten.github.io/rxjava-extras/apidocs/index.html).


TestingHelper
-----------------
This [helper class](src/main/java/com/github/davidmoten/rx/testing/TestingHelper.java) still in development. For a given named test the following variations  are tested:

* without backpressure
* intiial request maximum, no further request 
* initial request maximum, keep requesting single 
* backpressure, initial request 1, then by 0 and 1 
* backpressure, initial request 1, then by 1 
* backpressure, initial request 2, then by 2 
* backpressure, initial request 5, then by 5 
* backpressure, initial request 100, then by 100 
* backpressure, initial request 1000, then by 1000 
* backpressure, initial request 2, then Long.MAX_VALUE-1 (checks for request overflow)

Note that the above list no longer contains a check for negative request because that situation is covered by ```Subscriber.request``` throwing an ```IllegalArgumentException```.

For each variation the following aspects are tested:

* expected *onNext* items received
* unsubscribe from source occurs (for completion, error or explicit downstream unsubscription (optional))
* unsubscribe from downstream subscriber occurs
* ```onCompleted``` called (if unsubscribe not requested before completion and no errors expected)
* if ```onCompleted``` expected is only called once
* ```onError``` not called unless error expected
* if error expected ```onCompleted``` not called after ```onError```
* should not deliver more than requested

An example that tests all of the above variations and aspects for the ```Observable.count()``` method:

```java
import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;

import com.github.davidmoten.rx.testing.TestingHelper;

public class CountTest extends TestCase {

    public static TestSuite suite() {

        return TestingHelper
                .function(o -> o.count())
                // test empty
                .name("testCountOfEmptyReturnsZero")
                .fromEmpty()
                .expect(0)
                // test error
                .name("testCountErrorReturnsError")
                .fromError()
                .expectError()
                // test error after some emission
                .name("testCountErrorAfterTwoEmissionsReturnsError")
                .fromErrorAfter(5, 6)
                .expectError()
                // test non-empty count
                .name("testCountOfTwoReturnsTwo")
                .from(5, 6)
                .expect(2)
                // test single input
                .name("testCountOfOneReturnsOne")
                .from(5)
                .expect(1)
                // count many
                .name("testCountOfManyDoesNotGiveStackOverflow")
                .from(Observable.range(1, 1000000))
                .expect(1000000)
                // get test suites
                .testSuite(TestingHelperCountTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }

}
```

When you run ```CountTest``` as a JUnit test in Eclipse you see the test variations described as below:

<img src="src/docs/eclipse-junit.png?raw=true" />

An asynchronous example with ```OperatorMerge``` is below. Note the specific control of the wait times. For synchronous transformations the wait times can be left at their defaults:

```java
public class TestingHelperMergeTest extends TestCase {

    private static final Observable<Integer> MERGE_WITH = 
        Observable.from(asList(7, 8, 9)).subscribeOn(Schedulers.computation());

    public static TestSuite suite() {

        return TestingHelper
                .function(o->o.mergeWith(MERGE_WITH).subscribeOn(Schedulers.computation())
                .waitForUnsubscribe(100, TimeUnit.MILLISECONDS)
                .waitForTerminalEvent(10, TimeUnit.SECONDS)
                .waitForMoreTerminalEvents(100, TimeUnit.MILLISECONDS)
                // test empty
                .name("testEmptyWithOtherReturnsOther")
                .fromEmpty()
                .expect(7, 8, 9)
                // test error
                .name("testMergeErrorReturnsError")
                .fromError()
                .expectError()
                // test error after items
                .name("testMergeErrorAfter2ReturnsError")
                .fromErrorAfter(1, 2)
                .expectError()
                // test non-empty count
                .name("testTwoWithOtherReturnsTwoAndOtherInAnyOrder")
                .from(1, 2)
                .expectAnyOrder(1, 7, 8, 9, 2)
                // test single input
                .name("testOneWithOtherReturnsOneAndOtherInAnyOrder").from(1)
                .expectAnyOrder(7, 1, 8, 9)
                // unsub before completion
                .name("testTwoWithOtherUnsubscribedAfterOneReturnsOneItemOnly").from(1, 2)
                .unsubscribeAfter(1).expectSize(1)
                // get test suites
                .testSuite(TestingHelperMergeTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }
}
```

How to use OperatorUnsubscribeEagerly
---------------------------------------

```java 
Observable<T> o;

Observable<T> stream = 
      o.lift(OperatorUnsubscribeEagerly.<T>instance());
```

Checked
------------------

Checked exceptions can be annoying. If you are happy to wrap a checked exception with a ```RuntimeException``` then the function and action helpers in ```Checked``` are great:

Instead of 
```java
OutputStream os =  ...;
Observable<String> source = ...;
source.doOnNext(s -> {
	    try {
	        os.write(s.getBytes());
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }
    })
    .subscribe();
```

you can write:
```java
source.doOnNext(Checked.a1(s -> os.write(s.getBytes())))
      .subscribe();
```

