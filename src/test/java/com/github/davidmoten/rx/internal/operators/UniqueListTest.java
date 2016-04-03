package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Ignore;
import org.junit.Test;

import com.google.testing.threadtester.AnnotatedTestRunner;
import com.google.testing.threadtester.MethodOption;
import com.google.testing.threadtester.ThreadedAfter;
import com.google.testing.threadtester.ThreadedBefore;
import com.google.testing.threadtester.ThreadedMain;
import com.google.testing.threadtester.ThreadedSecondary;

public class UniqueListTest {

	private static final String HELLO = "Hello";

	private volatile UniqueList<String> uniqueList;

	@Ignore
	@Test(expected = RuntimeException.class)
	public void testPutIfAbsent() {
		System.out.printf("In testPutIfAbsent\n");
		// Create an AnnotatedTestRunner that will run the threaded tests
		// defined in this
		// class. We want to test the behaviour of the private method
		// "putIfAbsentInternal" so
		// we need to specify it by name using runner.setMethodOption()
		AnnotatedTestRunner runner = new AnnotatedTestRunner();
		HashSet<String> methods = new HashSet<String>();
		runner.setMethodOption(MethodOption.ALL_METHODS, methods);
		runner.setDebug(true);
		runner.runTests(this.getClass(), UniqueList.class);
	}

	@ThreadedBefore
	public void before() {
		// Set up a new UniqueList instance for the test
		uniqueList = new UniqueList<String>();
		System.out.printf("Created new list\n");
	}

	@ThreadedMain
	public void main() {
		// Add a new element to the list in the main test thread
		uniqueList.putIfAbsent(HELLO);
	}

	@ThreadedSecondary
	public void secondary() {
		// Add a new element to the list in the secondary test thread
		uniqueList.putIfAbsent(HELLO);
	}

	@ThreadedAfter
	public void after() {
		// If UniqueList is behaving correctly, it should only contain
		// a single copy of HELLO
		assertEquals(1, uniqueList.size());
		assertTrue(uniqueList.contains(HELLO));
	}
}