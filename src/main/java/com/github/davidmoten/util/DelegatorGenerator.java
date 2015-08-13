package com.github.davidmoten.util;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
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
                method.toGenericString();
                System.out.println(sharedToGenericString(method));
            }
        }
    }

    static String sharedToGenericString(Method m) {
        try {

            StringBuilder sb = new StringBuilder();

            int modifierMask = Modifier.methodModifiers();
            printModifiersIfNonzero(m, sb, modifierMask, m.isDefault());

            TypeVariable<?>[] typeparms = m.getTypeParameters();
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

            specificToGenericStringHeader(m, sb);

            sb.append('(');
            Type[] params = m.getGenericParameterTypes();
            for (int j = 0; j < params.length; j++) {
                String param = params[j].getTypeName();
                if (m.isVarArgs() && (j == params.length - 1)) // replace T[]
                                                               // with
                                                               // T...
                    param = param.replaceFirst("\\[\\]$", "...");
                sb.append(param);
                sb.append(' ');
                sb.append(m.getParameters()[j].getName());
                if (j < (params.length - 1))
                    sb.append(',');
            }
            sb.append(')');
            Type[] exceptions = m.getGenericExceptionTypes();
            if (exceptions.length > 0) {
                sb.append(" throws ");
                for (int k = 0; k < exceptions.length; k++) {
                    sb.append((exceptions[k] instanceof Class) ? ((Class) exceptions[k]).getName()
                            : exceptions[k].toString());
                    if (k < (exceptions.length - 1))
                        sb.append(',');
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "<" + e + ">";
        }
    }

    private static void specificToGenericStringHeader(Method m, StringBuilder sb) {
        Type genRetType = m.getGenericReturnType();
        sb.append(genRetType.getTypeName()).append(' ');
        // sb.append(m.getDeclaringClass().getTypeName()).append('.');
        sb.append(m.getName());
    }

    private static void printModifiersIfNonzero(Method m, StringBuilder sb, int mask,
            boolean isDefault) {
        int mod = m.getModifiers() & mask;

        if (mod != 0 && !isDefault) {
            sb.append(Modifier.toString(mod)).append(' ');
        } else {
            int access_mod = mod & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE);
            if (access_mod != 0)
                sb.append(Modifier.toString(access_mod)).append(' ');
            if (isDefault)
                sb.append("default ");
            mod = (mod & ~(Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE));
            if (mod != 0)
                sb.append(Modifier.toString(mod)).append(' ');
        }
    }

    public static void main(String[] args) {
        Map<Class<?>, String> map = new HashMap<Class<?>, String>();
        map.put(Observable.class, "rx.Ob");
        generate(map, new File("target"));
    }

}
