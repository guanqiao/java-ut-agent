package com.utagent.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoverageTrendTest {

    private CoverageTrend trend;

    @BeforeEach
    void setUp() {
        trend = new CoverageTrend();
    }

    @Test
    void testInitialState() {
        assertEquals(0, trend.getPointCount());
        assertNull(trend.getLatestPoint());
        assertNull(trend.getFirstPoint());
    }

    @Test
    void testAddPoint() {
        trend.addPoint(0.5, 0.4);
        
        assertEquals(1, trend.getPointCount());
        assertNotNull(trend.getLatestPoint());
        assertEquals(0.5, trend.getLatestPoint().lineCoverage(), 0.001);
        assertEquals(0.4, trend.getLatestPoint().branchCoverage(), 0.001);
    }

    @Test
    void testMultiplePoints() {
        trend.addPoint(0.3, 0.2);
        trend.addPoint(0.5, 0.4);
        trend.addPoint(0.7, 0.6);
        
        assertEquals(3, trend.getPointCount());
        assertEquals(0.3, trend.getFirstPoint().lineCoverage(), 0.001);
        assertEquals(0.7, trend.getLatestPoint().lineCoverage(), 0.001);
    }

    @Test
    void testTotalImprovement() {
        trend.addPoint(0.3, 0.2);
        trend.addPoint(0.5, 0.4);
        trend.addPoint(0.7, 0.6);
        
        double improvement = trend.getTotalImprovement();
        assertEquals(0.4, improvement, 0.001);
    }

    @Test
    void testAverageImprovementRate() {
        trend.addPoint(0.3, 0.2);
        trend.addPoint(0.5, 0.4);
        trend.addPoint(0.7, 0.6);
        
        double rate = trend.getAverageImprovementRate();
        assertEquals(0.2, rate, 0.001);
    }

    @Test
    void testMaxPointsLimit() {
        CoverageTrend limitedTrend = new CoverageTrend(5);
        
        for (int i = 0; i < 10; i++) {
            limitedTrend.addPoint(i * 0.1, i * 0.1);
        }
        
        assertEquals(5, limitedTrend.getPointCount());
    }

    @Test
    void testGenerateSparkline() {
        trend.addPoint(0.1, 0.1);
        trend.addPoint(0.3, 0.3);
        trend.addPoint(0.5, 0.5);
        trend.addPoint(0.7, 0.7);
        trend.addPoint(0.9, 0.9);
        
        String sparkline = trend.generateSparkline(10);
        assertNotNull(sparkline);
        assertFalse(sparkline.isEmpty());
    }

    @Test
    void testGenerateSparklineEmpty() {
        String sparkline = trend.generateSparkline(10);
        assertEquals("No data", sparkline);
    }

    @Test
    void testGenerateAsciiChart() {
        trend.addPoint(0.1, 0.1);
        trend.addPoint(0.3, 0.3);
        trend.addPoint(0.5, 0.5);
        trend.addPoint(0.7, 0.7);
        trend.addPoint(0.9, 0.9);
        
        String chart = trend.generateAsciiChart(20, 5);
        assertNotNull(chart);
        assertTrue(chart.contains("%"));
    }

    @Test
    void testGenerateAsciiChartEmpty() {
        String chart = trend.generateAsciiChart(20, 5);
        assertEquals("No data", chart);
    }

    @Test
    void testCoveragePointFormatted() {
        CoverageTrend.CoveragePoint point = new CoverageTrend.CoveragePoint(0.75, 0.60, java.time.Instant.now());
        
        String formatted = point.formatted();
        assertTrue(formatted.contains("75.0%"));
        assertTrue(formatted.contains("60.0%"));
    }

    @Test
    void testNegativeImprovement() {
        trend.addPoint(0.7, 0.6);
        trend.addPoint(0.5, 0.4);
        trend.addPoint(0.3, 0.2);
        
        double improvement = trend.getTotalImprovement();
        assertEquals(-0.4, improvement, 0.001);
    }

    @Test
    void testSinglePoint() {
        trend.addPoint(0.5, 0.4);
        
        assertEquals(1, trend.getPointCount());
        assertEquals(0.0, trend.getTotalImprovement(), 0.001);
        assertEquals(0.0, trend.getAverageImprovementRate(), 0.001);
    }
}
