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

    String sharedToGenericString(Method m, int modifierMask, boolean isDefault) {
        try {
            StringBuilder sb = new StringBuilder();

            printModifiersIfNonzero(m, sb, modifierMask, isDefault);

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

    void specificToGenericStringHeader(Method m, StringBuilder sb) {
        Type genRetType = m.getGenericReturnType();
        sb.append(genRetType.getTypeName()).append(' ');
        sb.append(m.getDeclaringClass().getTypeName()).append('.');
        sb.append(m.getName());
    }

    void printModifiersIfNonzero(Method m, StringBuilder sb, int mask, boolean isDefault) {
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
