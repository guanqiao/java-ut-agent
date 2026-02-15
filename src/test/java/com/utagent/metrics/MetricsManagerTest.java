package com.utagent.metrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsManagerTest {

    @BeforeEach
    void setUp() {
        MetricsManager.resetInstance();
    }

    @AfterEach
    void tearDown() {
        MetricsManager.resetInstance();
    }

    @Test
    void shouldCreateEnabledInstance() {
        MetricsManager manager = MetricsManager.getInstance(true);
        assertTrue(manager.isEnabled());
    }

    @Test
    void shouldCreateDisabledInstance() {
        MetricsManager manager = MetricsManager.getInstance(false);
        assertFalse(manager.isEnabled());
    }

    @Test
    void shouldReturnSingletonInstance() {
        MetricsManager instance1 = MetricsManager.getInstance();
        MetricsManager instance2 = MetricsManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void shouldRecordCacheMetrics() {
        MetricsManager manager = MetricsManager.getInstance(true);

        manager.recordCacheHit();
        manager.recordCacheHit();
        manager.recordCacheMiss();

        assertEquals(2.0 / 3.0, manager.getCacheHitRate(), 0.001);
    }

    @Test
    void shouldUpdateCacheSize() {
        MetricsManager manager = MetricsManager.getInstance(true);

        manager.updateCacheSize(100);

        // Cache size is tracked but not directly accessible
        // Just verify no exception is thrown
    }

    @Test
    void shouldRecordCounters() {
        MetricsManager manager = MetricsManager.getInstance(true);

        manager.incrementFilesProcessed();
        manager.incrementTestsGenerated(5);
        manager.incrementLlmCalls();
        manager.incrementLlmCacheHits();
        manager.incrementErrors();

        // Counters are tracked internally
        // Just verify no exception is thrown
    }

    @Test
    void shouldRecordTimers() {
        MetricsManager manager = MetricsManager.getInstance(true);

        var sample = manager.startGenerationTimer();
        manager.stopGenerationTimer(sample);

        sample = manager.startOptimizationTimer();
        manager.stopOptimizationTimer(sample);

        sample = manager.startLlmCallTimer();
        manager.stopLlmCallTimer(sample);

        sample = manager.startParseTimer();
        manager.stopParseTimer(sample);

        // Timers are tracked internally
        // Just verify no exception is thrown
    }

    @Test
    void shouldRecordGenerationTime() {
        MetricsManager manager = MetricsManager.getInstance(true);

        manager.recordGenerationTime(() -> {
            // Simulate work
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Timer and counter should be updated
        // Just verify no exception is thrown
    }

    @Test
    void shouldHandleExceptionInGenerationTime() {
        MetricsManager manager = MetricsManager.getInstance(true);

        assertThrows(RuntimeException.class, () -> {
            manager.recordGenerationTime(() -> {
                throw new RuntimeException("Test exception");
            });
        });

        // Error counter should be incremented
        // Just verify no exception is thrown during exception handling
    }

    @Test
    void shouldScrapePrometheusFormat() {
        MetricsManager manager = MetricsManager.getInstance(true);

        String scrape = manager.scrape();

        assertNotNull(scrape);
        assertTrue(scrape.contains("# HELP") || scrape.contains("# TYPE"));
    }

    @Test
    void shouldReturnDisabledMessageWhenDisabled() {
        MetricsManager manager = MetricsManager.getInstance(false);

        String scrape = manager.scrape();

        assertEquals("# Metrics disabled", scrape);
    }

    @Test
    void shouldHandleActiveTasks() {
        MetricsManager manager = MetricsManager.getInstance(true);

        manager.incrementActiveGenerations();
        manager.incrementActiveOptimizations();

        manager.decrementActiveGenerations();
        manager.decrementActiveOptimizations();

        // Active task counters are tracked internally
        // Just verify no exception is thrown
    }
}
