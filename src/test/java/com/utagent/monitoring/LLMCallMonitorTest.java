package com.utagent.monitoring;

import com.utagent.llm.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LLMCallMonitorTest {

    private LLMCallMonitor monitor;

    @BeforeEach
    void setUp() {
        LLMCallMonitor.resetInstance();
        monitor = LLMCallMonitor.getInstance();
    }

    @Test
    void testSingleton() {
        LLMCallMonitor instance1 = LLMCallMonitor.getInstance();
        LLMCallMonitor instance2 = LLMCallMonitor.getInstance();
        
        assertSame(instance1, instance2);
    }

    @Test
    void testStartAndEndCall() {
        LLMCallMonitor.CallRecord record = monitor.startCall("OpenAI", "gpt-4", "test_generation");
        
        assertNotNull(record);
        assertEquals("OpenAI", record.getProvider());
        assertEquals("gpt-4", record.getModel());
        assertEquals("test_generation", record.getPromptType());
        assertFalse(record.isSuccess());
        
        TokenUsage usage = new TokenUsage(100, 50, 150);
        monitor.endCall(record, usage, "Test response");
        
        assertTrue(record.isSuccess());
        assertEquals(usage, record.getTokenUsage());
        assertEquals(1, monitor.getSuccessfulCalls());
        assertEquals(1, monitor.getTotalCalls());
    }

    @Test
    void testFailedCall() {
        LLMCallMonitor.CallRecord record = monitor.startCall("OpenAI", "gpt-4", "test_generation");
        monitor.failCall(record, "API error");
        
        assertFalse(record.isSuccess());
        assertEquals("API error", record.getErrorMessage());
        assertEquals(1, monitor.getFailedCalls());
    }

    @Test
    void testCallListener() {
        AtomicReference<LLMCallMonitor.CallRecord> notified = new AtomicReference<>();
        monitor.addCallListener(notified::set);
        
        LLMCallMonitor.CallRecord record = monitor.startCall("OpenAI", "gpt-4", "test");
        monitor.endCall(record, new TokenUsage(100, 50, 150), "response");
        
        assertNotNull(notified.get());
        assertEquals(record, notified.get());
    }

    @Test
    void testTokenTracking() {
        LLMCallMonitor.CallRecord record1 = monitor.startCall("OpenAI", "gpt-4", "test1");
        monitor.endCall(record1, new TokenUsage(100, 50, 150), "response1");
        
        LLMCallMonitor.CallRecord record2 = monitor.startCall("OpenAI", "gpt-4", "test2");
        monitor.endCall(record2, new TokenUsage(200, 100, 300), "response2");
        
        assertEquals(450, monitor.getTotalTokens());
        assertEquals(300, monitor.getTotalPromptTokens());
        assertEquals(150, monitor.getTotalCompletionTokens());
    }

    @Test
    void testLatencyTracking() throws InterruptedException {
        LLMCallMonitor.CallRecord record = monitor.startCall("OpenAI", "gpt-4", "test");
        Thread.sleep(10);
        monitor.endCall(record, new TokenUsage(100, 50, 150), "response");
        
        assertTrue(record.getLatencyMs() >= 10);
        assertTrue(monitor.getTotalLatencyMs() >= 10);
        assertTrue(monitor.getAverageLatencyMs() >= 10);
    }

    @Test
    void testEstimatedCost() {
        LLMCallMonitor.CallRecord record = monitor.startCall("OpenAI", "gpt-4", "test");
        monitor.endCall(record, new TokenUsage(1000, 500, 1500), "response");
        
        double cost = monitor.getEstimatedCost(0.01, 0.03);
        assertEquals(0.025, cost, 0.001);
    }

    @Test
    void testEstimatedCostDefault() {
        LLMCallMonitor.CallRecord record = monitor.startCall("OpenAI", "gpt-4", "test");
        monitor.endCall(record, new TokenUsage(1000, 500, 1500), "response");
        
        double cost = monitor.getEstimatedCostDefault();
        assertEquals(0.025, cost, 0.001);
    }

    @Test
    void testCallHistory() {
        LLMCallMonitor.CallRecord record1 = monitor.startCall("OpenAI", "gpt-4", "test1");
        monitor.endCall(record1, new TokenUsage(100, 50, 150), "response1");
        
        LLMCallMonitor.CallRecord record2 = monitor.startCall("OpenAI", "gpt-4", "test2");
        monitor.endCall(record2, new TokenUsage(200, 100, 300), "response2");
        
        var history = monitor.getCallHistory();
        assertEquals(2, history.size());
    }

    @Test
    void testStatistics() {
        LLMCallMonitor.CallRecord record1 = monitor.startCall("OpenAI", "gpt-4", "test1");
        monitor.endCall(record1, new TokenUsage(100, 50, 150), "response1");
        
        LLMCallMonitor.CallRecord record2 = monitor.startCall("OpenAI", "gpt-4", "test2");
        monitor.failCall(record2, "error");
        
        LLMCallMonitor.Statistics stats = monitor.getStatistics();
        
        assertEquals(2, stats.totalCalls());
        assertEquals(1, stats.successfulCalls());
        assertEquals(1, stats.failedCalls());
        assertEquals(150, stats.totalTokens());
    }

    @Test
    void testStatisticsFormatting() {
        LLMCallMonitor.CallRecord record = monitor.startCall("OpenAI", "gpt-4", "test");
        monitor.endCall(record, new TokenUsage(1000000, 500000, 1500000), "response");
        
        LLMCallMonitor.Statistics stats = monitor.getStatistics();
        
        assertEquals("1.5M", stats.getFormattedTokens());
    }

    @Test
    void testReset() {
        LLMCallMonitor.CallRecord record = monitor.startCall("OpenAI", "gpt-4", "test");
        monitor.endCall(record, new TokenUsage(100, 50, 150), "response");
        
        assertEquals(1, monitor.getTotalCalls());
        
        monitor.reset();
        
        assertEquals(0, monitor.getTotalCalls());
        assertEquals(0, monitor.getSuccessfulCalls());
        assertEquals(0, monitor.getTotalTokens());
    }

    @Test
    void testStreamChunkNotification() {
        AtomicReference<LLMCallMonitor.StreamingChunk> notified = new AtomicReference<>();
        monitor.addStreamListener(notified::set);
        
        monitor.notifyStreamChunk(1, "test chunk", false);
        
        assertNotNull(notified.get());
        assertEquals(1, notified.get().callId());
        assertEquals("test chunk", notified.get().chunk());
    }

    @Test
    void testStreamingDisabled() {
        monitor.setStreamingEnabled(false);
        
        AtomicReference<LLMCallMonitor.StreamingChunk> notified = new AtomicReference<>();
        monitor.addStreamListener(notified::set);
        
        monitor.notifyStreamChunk(1, "test chunk", false);
        
        assertNull(notified.get());
    }

    @Test
    void testCallRecordCost() {
        LLMCallMonitor.CallRecord record = monitor.startCall("OpenAI", "gpt-4", "test");
        TokenUsage usage = new TokenUsage(1000, 500, 1500);
        monitor.endCall(record, usage, "response");
        
        double cost = record.getEstimatedCost(0.01, 0.03);
        assertEquals(0.025, cost, 0.001);
    }

    @Test
    void testCurrentCall() {
        LLMCallMonitor.CallRecord record = monitor.startCall("OpenAI", "gpt-4", "test");
        
        assertEquals(record, monitor.getCurrentCall());
        
        monitor.endCall(record, new TokenUsage(100, 50, 150), "response");
        
        assertNull(monitor.getCurrentCall());
    }
}
