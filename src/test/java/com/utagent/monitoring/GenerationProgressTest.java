package com.utagent.monitoring;

import com.utagent.llm.TokenUsage;
import com.utagent.model.CoverageReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GenerationProgressTest {

    private GenerationProgress progress;

    @BeforeEach
    void setUp() {
        progress = new GenerationProgress("TestFile.java", "TestClass");
    }

    @Test
    void testInitialState() {
        assertEquals("TestFile.java", progress.getSourceFile());
        assertEquals("TestClass", progress.getClassName());
        assertEquals(GenerationPhase.INITIALIZING, progress.getCurrentPhase());
        assertEquals(0, progress.getCurrentIteration());
        assertEquals(0, progress.getTotalTokens());
        assertEquals(0.0, progress.getCurrentCoverage());
        assertFalse(progress.isComplete());
        assertFalse(progress.isFailed());
    }

    @Test
    void testSetPhase() {
        AtomicReference<GenerationProgress> notified = new AtomicReference<>();
        progress.addListener(p -> notified.set(p));

        progress.setPhase(GenerationPhase.PARSING);
        
        assertEquals(GenerationPhase.PARSING, progress.getCurrentPhase());
        assertNotNull(notified.get());
    }

    @Test
    void testSetPhaseWithMessage() {
        progress.setPhase(GenerationPhase.TEST_GENERATION, "Generating tests");
        
        assertEquals(GenerationPhase.TEST_GENERATION, progress.getCurrentPhase());
        assertEquals("Generating tests", progress.getCurrentMessage());
    }

    @Test
    void testSetPhaseWithMessageAndDetail() {
        progress.setPhase(GenerationPhase.LLM_CALL, "Calling API", "Using GPT-4");
        
        assertEquals(GenerationPhase.LLM_CALL, progress.getCurrentPhase());
        assertEquals("Calling API", progress.getCurrentMessage());
        assertEquals("Using GPT-4", progress.getCurrentDetail());
    }

    @Test
    void testIterationTracking() {
        progress.setMaxIterations(10);
        assertEquals(10, progress.getMaxIterations());

        progress.incrementIteration();
        assertEquals(1, progress.getCurrentIteration());

        progress.setIteration(5);
        assertEquals(5, progress.getCurrentIteration());
    }

    @Test
    void testFileTracking() {
        progress.setTotalFiles(5);
        assertEquals(5, progress.getTotalFiles());

        progress.incrementFilesProcessed();
        assertEquals(1, progress.getFilesProcessed());
    }

    @Test
    void testTokenTracking() {
        TokenUsage usage = new TokenUsage(100, 50, 150);
        progress.addTokenUsage(usage);
        
        assertEquals(150, progress.getTotalTokens());
        assertEquals(100, progress.getPromptTokens());
        assertEquals(50, progress.getCompletionTokens());

        progress.addTokenUsage(new TokenUsage(50, 25, 75));
        assertEquals(225, progress.getTotalTokens());
    }

    @Test
    void testLlmCallTracking() {
        progress.incrementLlmCalls();
        assertEquals(1, progress.getLlmCalls());
        
        progress.incrementLlmCalls();
        assertEquals(2, progress.getLlmCalls());
    }

    @Test
    void testCoverageTracking() {
        progress.setCoverage(0.75, 0.60);
        
        assertEquals(0.75, progress.getCurrentCoverage(), 0.001);
        assertEquals(0.60, progress.getBranchCoverage(), 0.001);
    }

    @Test
    void testTargetCoverage() {
        progress.setTargetCoverage(0.9);
        assertEquals(0.9, progress.getTargetCoverage(), 0.001);
    }

    @Test
    void testErrorHandling() {
        Exception error = new RuntimeException("Test error");
        progress.setError(error);
        
        assertTrue(progress.isFailed());
        assertEquals(GenerationPhase.FAILED, progress.getCurrentPhase());
        assertEquals(error, progress.getLastError());
    }

    @Test
    void testComplete() {
        progress.complete();
        
        assertTrue(progress.isComplete());
        assertEquals(GenerationPhase.COMPLETED, progress.getCurrentPhase());
    }

    @Test
    void testOverallProgress() {
        progress.setPhase(GenerationPhase.PARSING);
        double progress1 = progress.getOverallProgress();
        
        progress.setPhase(GenerationPhase.COMPLETED);
        double progress2 = progress.getOverallProgress();
        
        assertTrue(progress1 < progress2);
        assertEquals(1.0, progress2, 0.001);
    }

    @Test
    void testIterationProgress() {
        progress.setMaxIterations(10);
        progress.setIteration(5);
        
        assertEquals(0.5, progress.getIterationProgress(), 0.001);
    }

    @Test
    void testEstimatedCost() {
        progress.addTokenUsage(new TokenUsage(1000, 500, 1500));
        
        double cost = progress.getEstimatedCost(0.01, 0.03);
        assertEquals(0.025, cost, 0.001);
    }

    @Test
    void testFormattedDuration() {
        String duration = progress.getFormattedDuration();
        assertNotNull(duration);
    }

    @Test
    void testTokensPerSecond() {
        progress.addTokenUsage(new TokenUsage(100, 50, 150));
        
        double tps = progress.getTokensPerSecond();
        assertTrue(tps >= 0);
    }

    @Test
    void testSnapshot() {
        progress.setPhase(GenerationPhase.TEST_GENERATION);
        progress.setIteration(3);
        progress.setCoverage(0.75, 0.60);
        
        GenerationProgress.Snapshot snapshot = progress.snapshot();
        
        assertEquals(GenerationPhase.TEST_GENERATION, snapshot.phase());
        assertEquals(3, snapshot.iteration());
        assertEquals(0.75, snapshot.coverage(), 0.001);
    }

    @Test
    void testTerminalState() {
        assertFalse(progress.isTerminal());
        
        progress.complete();
        assertTrue(progress.isTerminal());
    }

    @Test
    void testMessages() {
        progress.addMessage("First message");
        progress.addMessage("Second message");
        
        var messages = progress.getMessages();
        assertEquals(2, messages.size());
        assertTrue(messages.contains("First message"));
    }
}
