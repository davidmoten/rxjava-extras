package com.github.davidmoten.util;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;

public final class DelegatorGenerator {

    public static void generate(Map<Class<?>, String> map, File directory) {
        for (Class<?> cls : map.keySet()) {
            String className = cls.getCanonicalName();
            String newClassName = map.get(cls);
            for (Method method : cls.getDeclaredMethods()) {
                System.out.println(method.toGenericString());
                String mods = Modifier.toString(method.getModifiers());
                String g = method.getGenericReturnType().toString();
                for (Class<?> c : map.keySet()) {
                    g = g.replaceAll("\\b" + c.getCanonicalName() + "\\b", map.get(c));
                }

                String typeParams = typeParameters(method);

                System.out.format("%s %s %s %s(\n", mods, typeParams, g, method.getName());
            }
        }
    }

    private static String typeParameters(Method method) {
        String typeParams;
        StringBuilder sb = new StringBuilder();
        TypeVariable<?>[] typeparms = method.getTypeParameters();
        if (typeparms.length > 0) {
            boolean first = true;
            sb.append('<');
            for (TypeVariable<?> typeparm : typeparms) {
                if (!first)
                    sb.append(',');
                // Class objects can't occur here; no need to test
                // and call Class.getName().
                sb.append(typeparm.toString());
                first = false;
            }
            sb.append("> ");
        }
        typeParams = sb.toString();
        return typeParams;
    }

    public static void main(String[] args) {
        Map<Class<?>, String> map = new HashMap<Class<?>, String>();
        map.put(Observable.class, "rx.Ob");
        generate(map, new File("target"));
    }

}
