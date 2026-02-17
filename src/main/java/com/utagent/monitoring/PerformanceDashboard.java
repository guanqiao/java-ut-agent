package com.utagent.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerformanceDashboard {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceDashboard.class);

    private final Map<String, List<Long>> executionTimes;
    private final Map<String, Long> baselines;
    private int executionCount;

    public PerformanceDashboard() {
        this.executionTimes = new HashMap<>();
        this.baselines = new HashMap<>();
        this.executionCount = 0;
    }

    public void recordExecution(String testClass, String testName, long executionTime) {
        String key = testClass + "." + testName;
        executionTimes.computeIfAbsent(key, k -> new ArrayList<>()).add(executionTime);
        executionCount++;
        logger.debug("Recorded execution: {} - {}ms", key, executionTime);
    }

    public int getExecutionCount() {
        return executionCount;
    }

    public double getAverageExecutionTime(String testClass, String testName) {
        String key = testClass + "." + testName;
        List<Long> times = executionTimes.get(key);
        if (times == null || times.isEmpty()) {
            return 0.0;
        }
        return times.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    public List<SlowTestInfo> getSlowTests(long thresholdMs) {
        List<SlowTestInfo> slowTests = new ArrayList<>();
        
        for (Map.Entry<String, List<Long>> entry : executionTimes.entrySet()) {
            String key = entry.getKey();
            List<Long> times = entry.getValue();
            long avgTime = (long) times.stream().mapToLong(Long::longValue).average().orElse(0);
            
            if (avgTime > thresholdMs) {
                String[] parts = key.split("\\.");
                slowTests.add(new SlowTestInfo(parts[0], parts[1], avgTime));
            }
        }
        
        return slowTests;
    }

    public long getTotalExecutionTime() {
        return executionTimes.values().stream()
            .flatMap(List::stream)
            .mapToLong(Long::longValue)
            .sum();
    }

    public double getThroughput(long timeWindowMs) {
        if (timeWindowMs <= 0) {
            return 0.0;
        }
        return (double) executionCount / (timeWindowMs / 1000.0);
    }

    public ExecutionStats getStats(String testClass, String testName) {
        String key = testClass + "." + testName;
        List<Long> times = executionTimes.get(key);
        
        if (times == null || times.isEmpty()) {
            return new ExecutionStats(0, 0, 0, 0.0, 0);
        }
        
        int count = times.size();
        long min = times.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = times.stream().mapToLong(Long::longValue).max().orElse(0);
        double average = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long total = times.stream().mapToLong(Long::longValue).sum();
        
        return new ExecutionStats(count, min, max, average, total);
    }

    public String generateSummaryReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Performance Dashboard Summary ===\n\n");
        
        for (Map.Entry<String, List<Long>> entry : executionTimes.entrySet()) {
            String key = entry.getKey();
            List<Long> times = entry.getValue();
            double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
            
            report.append(String.format("%s: avg=%.2fms, count=%d\n", key, avg, times.size()));
        }
        
        report.append(String.format("\nTotal executions: %d\n", executionCount));
        report.append(String.format("Total time: %dms\n", getTotalExecutionTime()));
        
        return report.toString();
    }

    public String generateHtmlReport() {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Performance Dashboard</title></head><body>\n");
        html.append("<h1>Performance Dashboard</h1>\n");
        html.append("<table border=\"1\">\n");
        html.append("<tr><th>Test</th><th>Avg Time (ms)</th><th>Count</th></tr>\n");
        
        for (Map.Entry<String, List<Long>> entry : executionTimes.entrySet()) {
            String key = entry.getKey();
            List<Long> times = entry.getValue();
            double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
            
            html.append(String.format("<tr><td>%s</td><td>%.2f</td><td>%d</td></tr>\n", 
                key, avg, times.size()));
        }
        
        html.append("</table>\n");
        html.append("</body></html>");
        
        return html.toString();
    }

    public String generateJsonReport() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"executions\": [\n");
        
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, List<Long>> entry : executionTimes.entrySet()) {
            String key = entry.getKey();
            List<Long> times = entry.getValue();
            double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
            
            entries.add(String.format("    {\"test\": \"%s\", \"avgTime\": %.2f, \"count\": %d}", 
                key, avg, times.size()));
        }
        
        json.append(String.join(",\n", entries));
        json.append("\n  ],\n");
        json.append(String.format("  \"totalExecutions\": %d,\n", executionCount));
        json.append(String.format("  \"totalTime\": %d\n", getTotalExecutionTime()));
        json.append("}");
        
        return json.toString();
    }

    public void setBaseline(String testClass, String testName, long baselineTime) {
        String key = testClass + "." + testName;
        baselines.put(key, baselineTime);
    }

    public List<PerformanceAlert> checkForRegressions(double threshold) {
        List<PerformanceAlert> alerts = new ArrayList<>();
        
        for (Map.Entry<String, Long> baseline : baselines.entrySet()) {
            String key = baseline.getKey();
            Long baselineTime = baseline.getValue();
            
            List<Long> times = executionTimes.get(key);
            if (times != null && !times.isEmpty()) {
                long lastTime = times.get(times.size() - 1);
                double ratio = (double) lastTime / baselineTime;
                
                if (ratio > threshold) {
                    String[] parts = key.split("\\.");
                    alerts.add(new PerformanceAlert(parts[0], parts[1], baselineTime, lastTime, threshold));
                }
            }
        }
        
        return alerts;
    }

    public List<FlakyTestInfo> detectFlakyTests(double varianceThreshold) {
        List<FlakyTestInfo> flakyTests = new ArrayList<>();
        
        for (Map.Entry<String, List<Long>> entry : executionTimes.entrySet()) {
            String key = entry.getKey();
            List<Long> times = entry.getValue();
            
            if (times.size() >= 3) {
                double mean = times.stream().mapToLong(Long::longValue).average().orElse(0);
                double variance = times.stream()
                    .mapToDouble(t -> Math.pow(t - mean, 2))
                    .average().orElse(0);
                double stdDev = Math.sqrt(variance);
                double cv = mean > 0 ? stdDev / mean : 0;
                
                if (cv > varianceThreshold) {
                    String[] parts = key.split("\\.");
                    flakyTests.add(new FlakyTestInfo(parts[0], parts[1], cv));
                }
            }
        }
        
        return flakyTests;
    }

    public PerformanceTrend getTrend(String testClass, String testName) {
        String key = testClass + "." + testName;
        List<Long> times = executionTimes.get(key);
        
        if (times == null || times.size() < 2) {
            return new PerformanceTrend(TrendDirection.STABLE, 0.0, 0.0);
        }
        
        double slope = calculateSlope(times);
        TrendDirection direction;
        
        if (slope > 5) {
            direction = TrendDirection.INCREASING;
        } else if (slope < -5) {
            direction = TrendDirection.DECREASING;
        } else {
            direction = TrendDirection.STABLE;
        }
        
        double confidence = Math.min(1.0, times.size() / 10.0);
        
        return new PerformanceTrend(direction, slope, confidence);
    }

    public double predictExecutionTime(String testClass, String testName, int futureExecutions) {
        String key = testClass + "." + testName;
        List<Long> times = executionTimes.get(key);
        
        if (times == null || times.isEmpty()) {
            return 0.0;
        }
        
        double slope = calculateSlope(times);
        double lastValue = times.get(times.size() - 1);
        
        return Math.max(0, lastValue + slope * futureExecutions);
    }

    private double calculateSlope(List<Long> times) {
        int n = times.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = times.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return 0;
        }
        
        return (n * sumXY - sumX * sumY) / denominator;
    }
}
