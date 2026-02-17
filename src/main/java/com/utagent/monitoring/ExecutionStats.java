package com.utagent.monitoring;

public class ExecutionStats {
    private final int count;
    private final long min;
    private final long max;
    private final double average;
    private final long total;

    public ExecutionStats(int count, long min, long max, double average, long total) {
        this.count = count;
        this.min = min;
        this.max = max;
        this.average = average;
        this.total = total;
    }

    public int getCount() {
        return count;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    public double getAverage() {
        return average;
    }

    public long getTotal() {
        return total;
    }
}
