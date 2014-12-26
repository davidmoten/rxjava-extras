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
* ```TestingHelper```

TestingHelper
-----------------
An example that performs 10 unit tests per named case:

```java
import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

import com.github.davidmoten.rx.testing.TestingHelper;

public class TestingHelperCountTest extends TestCase {

    private static final Func1<Observable<String>, Observable<Integer>> COUNT = new Func1<Observable<String>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<String> o) {
            return o.count();
        }
    };

    public static TestSuite suite() {

        return TestingHelper.function(COUNT)
        // test empty
                .name("testEmpty").fromEmpty().expect(0)
                // test non-empty count
                .name("testTwo").from("a", "b").expect(2)
                // test single input
                .name("testOne").from("a").expect(1)
                // unsub before completion
                .name("testTwoUnsubscribeAfterOne").from("a", "b", "c").expect(3)
                // get test suites
                .testSuite(TestingHelperCountTest.class);
    }

    public void testDummy() {
        // just here to fool eclipse
    }

}
```

Status: *released to Maven Central*

Maven site reports are [here](http://davidmoten.github.io/rxjava-extras/index.html) including [javadoc](http://davidmoten.github.io/rxjava-extras/apidocs/index.html).



