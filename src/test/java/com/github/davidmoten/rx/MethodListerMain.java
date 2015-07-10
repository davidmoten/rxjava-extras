package com.github.davidmoten.rx;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map.Entry;
import java.util.TreeMap;

import rx.Observable;

public class MethodListerMain {

    public static void main(String[] args) {
        TreeMap<String, String> map = new TreeMap<String, String>();
        for (Method method : Observable.class.getMethods()) {
            if (Modifier.isPublic(method.getModifiers())
                    && !Modifier.isNative(method.getModifiers()))
                map.put(method.getName(), method.toString());
        }
        System.out.println("| Method | Signature |");
        System.out.println("| ------ | --------- |");
        for (Entry<String, String> entry : map.entrySet()) {
            System.out.println("| "
                    + entry.getKey()
                    + " | "
                    + entry.getValue().replaceAll("\\w+\\.", "").replaceAll("public ", "")
                            .replaceAll("final ", "").replaceAll("Observable", "Ob") + " |");
        }
    }
}
