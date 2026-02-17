package com.utagent.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhaseTimingTest {

    private PhaseTiming timing;

    @BeforeEach
    void setUp() {
        timing = new PhaseTiming(GenerationPhase.TEST_GENERATION);
    }

    @Test
    void testInitialState() {
        assertEquals(GenerationPhase.TEST_GENERATION, timing.getPhase());
        assertFalse(timing.isCompleted());
        assertTrue(timing.getDurationMs() >= 0);
    }

    @Test
    void testComplete() throws InterruptedException {
        Thread.sleep(10);
        timing.complete();
        
        assertTrue(timing.isCompleted());
        assertTrue(timing.getDurationMs() >= 10);
    }

    @Test
    void testDurationSeconds() throws InterruptedException {
        Thread.sleep(15);
        timing.complete();
        
        assertTrue(timing.getDurationSeconds() >= 0.015);
    }

    @Test
    void testFormattedDuration() throws InterruptedException {
        Thread.sleep(10);
        timing.complete();
        
        String formatted = timing.getFormattedDuration();
        assertNotNull(formatted);
        assertTrue(formatted.contains("ms") || formatted.contains("s"));
    }

    @Test
    void testPercentageOfTotal() {
        timing.complete();
        
        double percentage = timing.getPercentageOfTotal(1000);
        assertTrue(percentage >= 0);
    }

    @Test
    void testToString() {
        String str = timing.toString();
        assertTrue(str.contains("Generating tests"));
    }

    @Test
    void testLongDuration() throws InterruptedException {
        Thread.sleep(10);
        timing.complete();
        
        long duration = timing.getDurationMs();
        assertTrue(duration >= 10);
    }
}
