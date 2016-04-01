rxjava-extras
=============

<a href="https://travis-ci.org/davidmoten/rxjava-extras"><img src="https://travis-ci.org/davidmoten/rxjava-extras.svg"/></a><br/>
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/rxjava-extras/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.github.davidmoten/rxjava-extras)<br/>
<!--[![Dependency Status](https://gemnasium.com/com.github.davidmoten/rxjava-extras.svg)](https://gemnasium.com/com.github.davidmoten/rxjava-extras)-->


Utilities for use with rxjava:

* `Functions.identity, alwaysTrue, alwaysFalse, constant, not`
* `Checked` provides lambda helpers for dealing with checked exceptions in functions and actions
* `TestingHelper`
* `RetryWhen` builder for use with `.retryWhen(Func1)` operator
* [`Transformers.toOperator`](http://davidmoten.github.io/rxjava-extras/apidocs/com/github/davidmoten/rx/Transformers.html#toOperator-rx.functions.Func1-)
* [`Transformers.mapWithIndex`](http://davidmoten.github.io/rxjava-extras/apidocs/com/github/davidmoten/rx/Transformers.html#mapWithIndex--)
* [`Transformers.stateMachine`](http://davidmoten.github.io/rxjava-extras/apidocs/com/github/davidmoten/rx/Transformers.html#stateMachine-rx.functions.Func0-rx.functions.Func3-rx.functions.Action2-)
* [`Transformers.orderedMerge`](http://davidmoten.github.io/rxjava-extras/apidocs/com/github/davidmoten/rx/Transformers.html#orderedMergeWith-rx.Observable-rx.functions.Func2-)
* [`Transformers.collectWhile`](http://davidmoten.github.io/rxjava-extras/apidocs/com/github/davidmoten/rx/Transformers.html#collectWhile-rx.functions.Func0-rx.functions.Action2-rx.functions.Func2-)
* [`Transformers.toListWhile`](http://davidmoten.github.io/rxjava-extras/apidocs/com/github/davidmoten/rx/Transformers.html#toListWhile-rx.functions.Func2-)
* [`Transformers.toListUntilChanged`](http://davidmoten.github.io/rxjava-extras/apidocs/com/github/davidmoten/rx/Transformers.html#toListUntilChanged--)
* `Transformers.collectStats` 
* [`Transformers.doOnFirst`](http://davidmoten.github.io/rxjava-extras/apidocs/com/github/davidmoten/rx/Transformers.html#doOnFirst-rx.functions.Action1-)
* `Transformers.doOnNth`
* `Transformers.ignoreElementsThen`
* `Transformers.sampleFirst`
* `Transformers.decode`
* [`Transformers.onBackpressureBufferToFile`](README.md#transformersonbackpressurebuffertofile) - uses MapDB to buffer items to cache backed by disk, supports weak/soft/hard/LRU reference cache
* `Serialized.read/write`
* `PublishSubjectSingleSubscriber`
* `OperatorUnsubscribeEagerly`
* `Bytes.from`
* `Bytes.unzip` unzips zip archives
* `Strings.from`
* `Strings.lines` - supports backpressure (not available in rxjava-string 1.0.1)
* `Strings.split` - supports backpressure (not available in rxjava-string 1.0.1)


Status: *released to Maven Central*

Maven site reports are [here](http://davidmoten.github.io/rxjava-extras/index.html) including [javadoc](http://davidmoten.github.io/rxjava-extras/apidocs/index.html).

Getting started
-----------------
Add this to your pom.xml:

```xml
<dependency>
  <groupId>com.github.davidmoten</groupId>
  <artifactId>rxjava-extras</artifactId>
  <version>0.7</version>
</dependency>
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

Serialized
------------------
To read serialized objects from a file:

```java
Observable<Item> items = Serialized.read(file);
```

To write an Observable to a file:

```java
Serialized.write(observable, file).subscribe();
```

### Kryo
`Serialized` also has support for the very fast serialization library [kryo](https://github.com/EsotericSoftware/kryo). Unlike standard Java serialization *Kryo* can also serialize/deserialize objects that don't implement `Serializable`. 

Add this to your pom.xml:

```xml
<dependency>
    <groupId>com.esotericsoftware</groupId>
    <artifactId>kryo</artifactId>
    <version>3.0.3</version>
</dependency>
```

For example,

To read:
```java
Observable<Item> items = Serialized.kryo().read(file);
```

To write:
```java
Serialized.write(observable, file).subscribe();
```

You can also call `Serialized.kryo(kryo)` to use an instance of `Kryo` that you have configured specially. 

Transformers.toListWhile
---------------------------
You may want to group emissions from an Observable into lists of variable size. This can be achieved safely using `toListWhile`.

As an example from a sequence of temperatures lets group the sub-zero and zero or above temperatures into contiguous lists:

```java
Observable.just(10, 5, 2, -1, -2, -5, -1, 2, 5, 6)
    .compose(Transformers.toListWhile( 
        (list, t) -> list.isEmpty() 
            || Math.signum(list.get(0)) < 0 && Math.signum(t) < 0
            || Math.signum(list.get(0)) >= 0 && Math.signum(t) >= 0)
    .forEach(System.out::println);
```
produces
```
[10, 5, 2]
[-1, -2, -5, -1]
[2, 5, 6]
```

Tranformers.collectWhile
-------------------------
Behaves as per `toListWhile` but allows control over the data structure used. 

<img src="src/docs/collectWhile.png?raw=true" />

Transformers.stateMachine
--------------------------
Custom operators are difficult things to get right in RxJava mainly because of the complexity of supporting backpressure efficiently. `Transformers.stateMachine` enables a custom operator implementation when:

* each source emission is mapped to 0 to many emissions (of a different type perhaps) to downstream but those emissions are calculated based on accumulated state

An example of such a transformation might be from a list of temperatures you only want to emit sequential values that are less than zero but are part of a sub-zero sequence at least 1 hour in duration. You could use `toListWhile` above but `Transformers.stateMachine` offers the additional efficiency that it will immediately emit temperatures as soon as the duration criterion is met. 

To implement this example, suppose the source is half-hourly temperature measurements:

```java
static class State {
     final List<Double> list;
     final boolean reachedThreshold;
     State(List<Double> list, boolean reachedThreshold) {
         this.list = list; 
         this.reachedThreshold = reachedThreshold;
     }
}

int MIN_SEQUENCE_LENGTH = 2;
Observable.just(10, 5, 2, -1, -2, -5, -1, 2, 5, 6)
    .compose(Transformers.stateMachine(
        () -> new State(new ArrayList<>(), false),
        (state,t,subscriber) -> {
                if (t < 0) {
                    if (state.reachedThreshold) {
                        if (subscriber.isUnsubscribed()) {
                            return null;
                        }
                        subscriber.onNext(t);
                        return s;
                     } else if (state.list.size() == MIN_SEQUENCE_LENGTH - 1) {
                        for (Double temperature: list) {
                        	if (subscriber.isUnsubscribed()){
                                return null;
                        	}
                    	    subscriber.onNext(temperature);
                        }
                        return new State(null, true);
                     } else {
                        List<Double> list = new ArrayList<>(state.list);
                        list.add(t);
                        return new State(list, false);
                     }
                } else {
                    return new State(new ArrayList<>(), false);
                }
            }) 
    .forEach(System.out::println);
```

RetryWhen
----------------------
A common use case for `.retry()` is some sequence of actions that are attempted and then after a delay a retry is attempted. RxJava does not provide 
first class support for this use case but the building blocks are there with the `.retryWhen()` method. `RetryWhen` offers static methods that build a `Func1` for use with `Observable.retryWhen()`.

### Retry after a constant delay

```java
observable.retryWhen(
    RetryWhen.delay(10, TimeUnit.SECONDS).build());
```

### Retry after a constant delay with a maximum number of retries

```java
observable.retryWhen(
    RetryWhen.delay(10, TimeUnit.SECONDS)
        .maxRetries(10).build());
```

### Retry after custom delays

```java
//the length of waits determines number of retries
Observable<Long> delays = Observable.just(10L,20L,30L,30L,30L);
observable.retryWhen(
    RetryWhen.delays(delays, TimeUnit.SECONDS).build());
```

### Retry only for a particular exception

```java
observable.retryWhen(
    RetryWhen.retryWhenInstanceOf(IOException.class)
        .build());
```

Unzipping
-----------------------

Suppose you have a a zip file `file.zip` and you want to stream the lines of the file `doc.txt` extracted from the archive:

```java
Observable<String> lines = 
    Bytes.unzip(new File("file.zip"))
       .filter(entry -> entry.getName().equals("doc.txt"))
       .concatMap(entry -> Strings.from(entry.getInputStream))
       .compose(o-> Strings.split(o, "\n"));
```
Note that above you don't need to worry about closing `entry.getInputStream()` because it is handled in the unsubscribe of the `Bytes.unzip` source.

You must process the emissions of `ZippedEntry` synchronously (don't replace the `concatMap()` with a `flatMap(...  .subscribeOn(Schedulers.computation())` for instance. This is because the `InputStream` of each `ZippedEntry` must be processed fullly (which could mean ignoring it of course) before moving on to the next one.


Transformers.orderedMerge
--------------------------
To merge two streams in order (according to a `Comparator`):

```java
source1.compose(Transformers.orderedMergeWith(source2, comparator));
```

Transformers.onBackpressureBufferToFile
----------------------------------------
As of 0.7.1-RC1, if you add a dependency for [MapDB](http://www.mapdb.org) you can offload an observable chain to disk to reduce memory pressure when you have a fast producer + slow consumer (or just to minimize memory usage). 

```xml
 <dependency>
    <groupId>org.mapdb</groupId>
    <artifactId>mapdb</artifactId>
    <version>1.0.9</version>
</dependency>
```
Here's an example:

```java
// define how the items in the source stream would be serialized
DataSerializer<String> serializer = new DataSerializer<String>() {

    @Override
    public void serialize(DataOutput output, String s) throws IOException {
        output.writeUTF(s);
    }

    @Override
    public String deserialize(DataInput input, int availableBytes) throws IOException {
        return input.readUTF();
    }
};

// write the source strings to a 
// disk-backed queue on the subscription
// thread and emit the items read from 
// the queue on the computation() scheduler.
Observable
  .just("a", "b", "c")
  .compose(
    Transformers.onBackpressureBufferToFile(
        serializer, Schedulers.computation()))
  ...
```
You can configure various options:

```java
Observable
  .just("a", "b", "c")
  .compose(
    Transformers.onBackpressureBufferToFile(
        serializer, 
        Schedulers.computation(), 
        Options
          .fileFactory(fileFactory)
          .cacheType(CacheType.SOFT_REF)
          .cacheSizeItems(1000)
          .storageSizeLimitMB(100)
          .delayError(false)
          .build()))
  ...
```

To use defaults (emits on `Schedulers.computation`):

```java
Observable
  .just("a", "b", "c")
  .compose(
    Transformers.onBackpressureBufferToFile(serializer);
```

`Options.fileFactory(Func0<File>)` specifies the method used to create the root temporary file used by the queue storage mechanism (MapDB). The default is a factory that calls `Files.createTempFile("bufferToFileDB`, "")`.

Caching options include `SOFT_REF`, `WEAK_REF`, `HARD_REF`, `LEAST_RECENTLY_USED`, and `NO_CACHE`. The default is `NO_CACHE`.

If storage size limit is exceeded then an `IOException` will be emitted by the stream. This is a critical error in that MapDB resources in memory may not be disposed of properly and files associated with the stream may not have been deleted on unsubscription. Don't count on graceful recovery from this scenario!


TestingHelper
-----------------
For a given named test the following variations  are tested:

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

