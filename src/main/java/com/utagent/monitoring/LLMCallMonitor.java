package com.utagent.monitoring;

import com.utagent.llm.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class LLMCallMonitor {

    private static final Logger logger = LoggerFactory.getLogger(LLMCallMonitor.class);
    
    private static volatile LLMCallMonitor instance;
    
    private final List<CallRecord> callHistory = new ArrayList<>();
    private final AtomicInteger totalCalls = new AtomicInteger(0);
    private final AtomicInteger successfulCalls = new AtomicInteger(0);
    private final AtomicInteger failedCalls = new AtomicInteger(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    private final AtomicLong totalPromptTokens = new AtomicLong(0);
    private final AtomicLong totalCompletionTokens = new AtomicLong(0);
    private final AtomicLong totalTokens = new AtomicLong(0);
    
    private final List<Consumer<CallRecord>> callListeners = new ArrayList<>();
    private final List<Consumer<StreamingChunk>> streamListeners = new ArrayList<>();
    
    private volatile CallRecord currentCall;
    private volatile boolean streamingEnabled = true;

    private LLMCallMonitor() {}

    public static LLMCallMonitor getInstance() {
        if (instance == null) {
            synchronized (LLMCallMonitor.class) {
                if (instance == null) {
                    instance = new LLMCallMonitor();
                }
            }
        }
        return instance;
    }

    public static void resetInstance() {
        instance = null;
    }

    public void addCallListener(Consumer<CallRecord> listener) {
        callListeners.add(listener);
    }

    public void addStreamListener(Consumer<StreamingChunk> listener) {
        streamListeners.add(listener);
    }

    public void setStreamingEnabled(boolean enabled) {
        this.streamingEnabled = enabled;
    }

    public CallRecord startCall(String provider, String model, String promptType) {
        CallRecord record = new CallRecord(
            totalCalls.incrementAndGet(),
            provider,
            model,
            promptType,
            Instant.now()
        );
        currentCall = record;
        logger.debug("LLM call #{} started: {} - {}", record.callId, provider, promptType);
        return record;
    }

    public void endCall(CallRecord record, TokenUsage tokenUsage, String responsePreview) {
        if (record == null) return;
        
        record.complete(tokenUsage, responsePreview);
        successfulCalls.incrementAndGet();
        totalLatencyMs.addAndGet(record.getLatencyMs());
        
        if (tokenUsage != null) {
            totalPromptTokens.addAndGet(tokenUsage.promptTokens());
            totalCompletionTokens.addAndGet(tokenUsage.completionTokens());
            totalTokens.addAndGet(tokenUsage.totalTokens());
        }
        
        synchronized (callHistory) {
            callHistory.add(record);
        }
        
        currentCall = null;
        
        for (Consumer<CallRecord> listener : callListeners) {
            try {
                listener.accept(record);
            } catch (Exception e) {
                logger.warn("Error in call listener", e);
            }
        }
        
        logger.debug("LLM call #{} completed: {}ms, {} tokens", 
            record.callId, record.getLatencyMs(), 
            tokenUsage != null ? tokenUsage.totalTokens() : 0);
    }

    public void failCall(CallRecord record, String errorMessage) {
        if (record == null) return;
        
        record.fail(errorMessage);
        failedCalls.incrementAndGet();
        totalLatencyMs.addAndGet(record.getLatencyMs());
        
        synchronized (callHistory) {
            callHistory.add(record);
        }
        
        currentCall = null;
        
        for (Consumer<CallRecord> listener : callListeners) {
            try {
                listener.accept(record);
            } catch (Exception e) {
                logger.warn("Error in call listener", e);
            }
        }
        
        logger.warn("LLM call #{} failed: {}", record.callId, errorMessage);
    }

    public void notifyStreamChunk(int callId, String chunk, boolean isComplete) {
        if (!streamingEnabled) return;
        
        StreamingChunk streamChunk = new StreamingChunk(callId, chunk, isComplete, Instant.now());
        
        for (Consumer<StreamingChunk> listener : streamListeners) {
            try {
                listener.accept(streamChunk);
            } catch (Exception e) {
                logger.warn("Error in stream listener", e);
            }
        }
    }

    public int getTotalCalls() {
        return totalCalls.get();
    }

    public int getSuccessfulCalls() {
        return successfulCalls.get();
    }

    public int getFailedCalls() {
        return failedCalls.get();
    }

    public long getTotalLatencyMs() {
        return totalLatencyMs.get();
    }

    public double getAverageLatencyMs() {
        int calls = successfulCalls.get();
        return calls > 0 ? (double) totalLatencyMs.get() / calls : 0;
    }

    public long getTotalPromptTokens() {
        return totalPromptTokens.get();
    }

    public long getTotalCompletionTokens() {
        return totalCompletionTokens.get();
    }

    public long getTotalTokens() {
        return totalTokens.get();
    }

    public double getEstimatedCost(double promptPricePer1k, double completionPricePer1k) {
        return (totalPromptTokens.get() * promptPricePer1k / 1000.0) +
               (totalCompletionTokens.get() * completionPricePer1k / 1000.0);
    }

    public double getEstimatedCostDefault() {
        return getEstimatedCost(0.01, 0.03);
    }

    public CallRecord getCurrentCall() {
        return currentCall;
    }

    public List<CallRecord> getCallHistory() {
        synchronized (callHistory) {
            return new ArrayList<>(callHistory);
        }
    }

    public Statistics getStatistics() {
        return new Statistics(
            totalCalls.get(),
            successfulCalls.get(),
            failedCalls.get(),
            getTotalLatencyMs(),
            getAverageLatencyMs(),
            totalPromptTokens.get(),
            totalCompletionTokens.get(),
            totalTokens.get(),
            getEstimatedCostDefault()
        );
    }

    public void reset() {
        synchronized (callHistory) {
            callHistory.clear();
        }
        totalCalls.set(0);
        successfulCalls.set(0);
        failedCalls.set(0);
        totalLatencyMs.set(0);
        totalPromptTokens.set(0);
        totalCompletionTokens.set(0);
        totalTokens.set(0);
        currentCall = null;
    }

    public static class CallRecord {
        private final int callId;
        private final String provider;
        private final String model;
        private final String promptType;
        private final Instant startTime;
        private Instant endTime;
        private TokenUsage tokenUsage;
        private String responsePreview;
        private String errorMessage;
        private boolean success;

        private CallRecord(int callId, String provider, String model, String promptType, Instant startTime) {
            this.callId = callId;
            this.provider = provider;
            this.model = model;
            this.promptType = promptType;
            this.startTime = startTime;
            this.success = false;
        }

        private void complete(TokenUsage tokenUsage, String responsePreview) {
            this.endTime = Instant.now();
            this.tokenUsage = tokenUsage;
            this.responsePreview = responsePreview != null && responsePreview.length() > 100 
                ? responsePreview.substring(0, 100) + "..." 
                : responsePreview;
            this.success = true;
        }

        private void fail(String errorMessage) {
            this.endTime = Instant.now();
            this.errorMessage = errorMessage;
            this.success = false;
        }

        public int getCallId() {
            return callId;
        }

        public String getProvider() {
            return provider;
        }

        public String getModel() {
            return model;
        }

        public String getPromptType() {
            return promptType;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public long getLatencyMs() {
            if (endTime != null) {
                return Duration.between(startTime, endTime).toMillis();
            }
            return Duration.between(startTime, Instant.now()).toMillis();
        }

        public TokenUsage getTokenUsage() {
            return tokenUsage;
        }

        public String getResponsePreview() {
            return responsePreview;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public double getEstimatedCost(double promptPricePer1k, double completionPricePer1k) {
            if (tokenUsage == null) return 0;
            return tokenUsage.estimatedCost(promptPricePer1k, completionPricePer1k);
        }
    }

    public record StreamingChunk(int callId, String chunk, boolean isComplete, Instant timestamp) {}

    public record Statistics(
        int totalCalls,
        int successfulCalls,
        int failedCalls,
        long totalLatencyMs,
        double averageLatencyMs,
        long totalPromptTokens,
        long totalCompletionTokens,
        long totalTokens,
        double estimatedCost
    ) {
        public String getFormattedLatency() {
            if (totalLatencyMs < 1000) {
                return totalLatencyMs + "ms";
            } else if (totalLatencyMs < 60000) {
                return String.format("%.1fs", totalLatencyMs / 1000.0);
            } else {
                long minutes = totalLatencyMs / 60000;
                long seconds = (totalLatencyMs % 60000) / 1000;
                return String.format("%dm %ds", minutes, seconds);
            }
        }

        public String getFormattedTokens() {
            if (totalTokens >= 1_000_000) {
                return String.format("%.1fM", totalTokens / 1_000_000.0);
            } else if (totalTokens >= 1_000) {
                return String.format("%.1fK", totalTokens / 1_000.0);
            }
            return String.valueOf(totalTokens);
        }
    }
}
