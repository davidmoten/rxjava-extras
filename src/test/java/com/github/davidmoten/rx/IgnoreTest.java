package com.github.davidmoten.rx;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;
import com.github.davidmoten.rx.Checked.A0;
import com.github.davidmoten.rx.Checked.A1;

public class IgnoreTest {

    @Test
    public void isUtilClass() {
        Asserts.assertIsUtilityClass(Ignore.class);
    }
    
    @Test
    public void ignore0DoesNotThrowException() {
        Ignore.a0(new A0() {

            @Override
            public void call() {
                throw new RuntimeException();
            }
            
        }).call();
    }
    
    @Test
    public void ignore1DoesNotThrowException() {
        Ignore.a1(new A1<Integer>() {

            @Override
            public void call(Integer n) {
                throw new RuntimeException();
            }
            
        }).call(1);
    }
    
}
