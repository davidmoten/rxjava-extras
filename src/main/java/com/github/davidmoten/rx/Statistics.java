package com.github.davidmoten.rx;

public final class Statistics {

    private final long count;
    private final double sumX;
    private final double sumX2;

    private Statistics(long count, double sumX, double sumX2) {
        this.count = count;
        this.sumX = sumX;
        this.sumX2 = sumX2;
    }

    public static Statistics create() {
        return new Statistics(0, 0, 0);
    }

    public Statistics add(Number number) {
        double x = number.doubleValue();
        return new Statistics(count + 1, sumX + x, sumX2 + x * x);
    }

    public long count() {
        return count;
    }

    public double sum() {
        return sumX;
    }

    public double sumSquares() {
        return sumX2;
    }

    public double mean() {
        return sumX / count;
    }

    public double sd() {
        double m = mean();
        return Math.sqrt(sumX2 / count - m * m);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Statistics [count=");
        builder.append(count);
        builder.append(", sum=");
        builder.append(sum());
        builder.append(", mean=");
        builder.append(mean());
        builder.append(", sd=");
        builder.append(sd());
        builder.append("]");
        return builder.toString();
    }
}