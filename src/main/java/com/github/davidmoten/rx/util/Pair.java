package com.github.davidmoten.rx.util;

public class Pair<T, S> {

    private final T a;
    private final S b;

    public Pair(T a, S b) {
        this.a = a;
        this.b = b;
    }

    public static <T, S> Pair<T, S> create(T t, S s) {
        return new Pair<T, S>(t, s);
    }

    public T a() {
        return a;
    }

    public S b() {
        return b;
    }

    public T left() {
        return a;
    }

    public S right() {
        return b;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((a == null) ? 0 : a.hashCode());
        result = prime * result + ((b == null) ? 0 : b.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Pair<?,?> other = (Pair<?,?>) obj;
        if (a == null) {
            if (other.a != null)
                return false;
        } else if (!a.equals(other.a))
            return false;
        if (b == null) {
            if (other.b != null)
                return false;
        } else if (!b.equals(other.b))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Pair [left=");
        builder.append(a);
        builder.append(", right=");
        builder.append(b);
        builder.append("]");
        return builder.toString();
    }
}
