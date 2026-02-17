package com.utagent.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenerationPhaseTest {

    @Test
    void testPhaseOrder() {
        assertTrue(GenerationPhase.INITIALIZING.getOrder() < GenerationPhase.PARSING.getOrder());
        assertTrue(GenerationPhase.PARSING.getOrder() < GenerationPhase.TEST_GENERATION.getOrder());
        assertTrue(GenerationPhase.OPTIMIZATION.getOrder() < GenerationPhase.COMPLETED.getOrder());
    }

    @Test
    void testTerminalPhases() {
        assertTrue(GenerationPhase.COMPLETED.isTerminal());
        assertTrue(GenerationPhase.FAILED.isTerminal());
        assertFalse(GenerationPhase.PARSING.isTerminal());
        assertFalse(GenerationPhase.TEST_GENERATION.isTerminal());
    }

    @Test
    void testProgressPercentage() {
        assertEquals(0.0, GenerationPhase.INITIALIZING.getProgressPercentage(), 0.01);
        
        double completedProgress = GenerationPhase.COMPLETED.getProgressPercentage();
        double failedProgress = GenerationPhase.FAILED.getProgressPercentage();
        
        assertTrue(completedProgress > 0);
        assertTrue(failedProgress > completedProgress);
        
        for (GenerationPhase phase : GenerationPhase.values()) {
            double progress = phase.getProgressPercentage();
            assertTrue(progress >= 0 && progress <= 100);
        }
    }

    @Test
    void testDisplayName() {
        assertNotNull(GenerationPhase.PARSING.getDisplayName());
        assertEquals("Parsing source code", GenerationPhase.PARSING.getDisplayName());
    }

    @Test
    void testIcon() {
        assertNotNull(GenerationPhase.COMPLETED.getIcon());
        assertEquals("✅", GenerationPhase.COMPLETED.getIcon());
        assertEquals("❌", GenerationPhase.FAILED.getIcon());
    }
}
