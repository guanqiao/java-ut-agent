package com.utagent.monitoring;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CoverageTrend {

    private final List<CoveragePoint> points = new ArrayList<>();
    private final int maxPoints;

    public CoverageTrend() {
        this(50);
    }

    public CoverageTrend(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public synchronized void addPoint(double lineCoverage, double branchCoverage) {
        addPoint(lineCoverage, branchCoverage, Instant.now());
    }

    public synchronized void addPoint(double lineCoverage, double branchCoverage, Instant timestamp) {
        points.add(new CoveragePoint(lineCoverage, branchCoverage, timestamp));
        
        while (points.size() > maxPoints) {
            points.remove(0);
        }
    }

    public synchronized List<CoveragePoint> getPoints() {
        return new ArrayList<>(points);
    }

    public synchronized int getPointCount() {
        return points.size();
    }

    public synchronized CoveragePoint getLatestPoint() {
        if (points.isEmpty()) return null;
        return points.get(points.size() - 1);
    }

    public synchronized CoveragePoint getFirstPoint() {
        if (points.isEmpty()) return null;
        return points.get(0);
    }

    public double getTotalImprovement() {
        if (points.size() < 2) return 0;
        CoveragePoint first = getFirstPoint();
        CoveragePoint latest = getLatestPoint();
        if (first == null || latest == null) return 0;
        return latest.lineCoverage() - first.lineCoverage();
    }

    public double getAverageImprovementRate() {
        if (points.size() < 2) return 0;
        return getTotalImprovement() / (points.size() - 1);
    }

    public String generateAsciiChart(int width, int height) {
        if (points.isEmpty()) {
            return "No data";
        }

        char[][] grid = new char[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = ' ';
            }
        }

        double minCov = points.stream()
            .mapToDouble(CoveragePoint::lineCoverage)
            .min()
            .orElse(0);
        double maxCov = points.stream()
            .mapToDouble(CoveragePoint::lineCoverage)
            .max()
            .orElse(1);
        
        if (maxCov == minCov) {
            maxCov = minCov + 0.1;
        }

        for (int i = 0; i < points.size(); i++) {
            CoveragePoint point = points.get(i);
            int x = (int) ((double) i / (points.size() - 1) * (width - 1));
            int y = (int) ((1 - (point.lineCoverage() - minCov) / (maxCov - minCov)) * (height - 1));
            
            x = Math.max(0, Math.min(width - 1, x));
            y = Math.max(0, Math.min(height - 1, y));
            
            grid[y][x] = '█';
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%.0f%%", maxCov * 100)).append(" ┤\n");
        
        for (int y = 0; y < height; y++) {
            if (y == 0) {
                sb.append(String.format("%5s", "")).append(" │");
            } else if (y == height - 1) {
                sb.append(String.format("%.0f%%", minCov * 100)).append(" ┤");
            } else {
                sb.append(String.format("%5s", "")).append(" │");
            }
            
            for (int x = 0; x < width; x++) {
                sb.append(grid[y][x]);
            }
            sb.append("\n");
        }
        
        sb.append(String.format("%5s", "")).append(" └");
        sb.append("─".repeat(width));
        sb.append("\n");

        return sb.toString();
    }

    public String generateSparkline(int width) {
        if (points.isEmpty()) {
            return "No data";
        }

        char[] sparkChars = {'▁', '▂', '▃', '▄', '▅', '▆', '▇', '█'};
        
        double min = points.stream()
            .mapToDouble(CoveragePoint::lineCoverage)
            .min()
            .orElse(0);
        double max = points.stream()
            .mapToDouble(CoveragePoint::lineCoverage)
            .max()
            .orElse(1);
        
        if (max == min) {
            max = min + 0.1;
        }

        StringBuilder sb = new StringBuilder();
        int step = Math.max(1, points.size() / width);
        
        for (int i = 0; i < points.size(); i += step) {
            CoveragePoint point = points.get(i);
            int charIndex = (int) ((point.lineCoverage() - min) / (max - min) * (sparkChars.length - 1));
            charIndex = Math.max(0, Math.min(sparkChars.length - 1, charIndex));
            sb.append(sparkChars[charIndex]);
        }

        return sb.toString();
    }

    public record CoveragePoint(double lineCoverage, double branchCoverage, Instant timestamp) {
        public String formatted() {
            return String.format("%.1f%% (branch: %.1f%%)", 
                lineCoverage * 100, branchCoverage * 100);
        }
    }
}
