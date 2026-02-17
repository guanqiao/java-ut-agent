package com.utagent.monitoring;

import com.utagent.llm.TokenUsage;
import com.utagent.model.CoverageReport;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class GenerationProgress {

    private final String sourceFile;
    private final String className;
    private final Instant startTime;
    
    private final AtomicReference<GenerationPhase> currentPhase = new AtomicReference<>(GenerationPhase.INITIALIZING);
    private final AtomicInteger currentIteration = new AtomicInteger(0);
    private final AtomicInteger maxIterations = new AtomicInteger(10);
    private final AtomicInteger filesProcessed = new AtomicInteger(0);
    private final AtomicInteger totalFiles = new AtomicInteger(1);
    
    private final AtomicLong totalTokens = new AtomicLong(0);
    private final AtomicLong promptTokens = new AtomicLong(0);
    private final AtomicLong completionTokens = new AtomicLong(0);
    private final AtomicInteger llmCalls = new AtomicInteger(0);
    
    private final AtomicReference<Double> currentCoverage = new AtomicReference<>(0.0);
    private final AtomicReference<Double> targetCoverage = new AtomicReference<>(0.8);
    private final AtomicReference<Double> branchCoverage = new AtomicReference<>(0.0);
    
    private final List<PhaseRecord> phaseHistory = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();
    
    private final Map<GenerationPhase, PhaseTiming> phaseTimings = new HashMap<>();
    private final CoverageTrend coverageTrend = new CoverageTrend();
    private final AtomicInteger testMethodsGenerated = new AtomicInteger(0);
    private final AtomicInteger testClassesGenerated = new AtomicInteger(0);
    
    private final List<Consumer<GenerationProgress>> listeners = new ArrayList<>();
    
    private volatile String currentMessage = "";
    private volatile String currentDetail = "";
    private volatile Throwable lastError;
    private volatile PhaseTiming currentPhaseTiming;

    public GenerationProgress(String sourceFile, String className) {
        this.sourceFile = sourceFile;
        this.className = className;
        this.startTime = Instant.now();
    }

    public void addListener(Consumer<GenerationProgress> listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Consumer<GenerationProgress> listener : listeners) {
            try {
                listener.accept(this);
            } catch (Exception e) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
        }
    }

    public synchronized void setPhase(GenerationPhase phase) {
        GenerationPhase oldPhase = currentPhase.get();
        if (oldPhase != phase) {
            if (currentPhaseTiming != null) {
                currentPhaseTiming.complete();
                phaseTimings.put(oldPhase, currentPhaseTiming);
            }
            
            if (oldPhase != GenerationPhase.INITIALIZING) {
                phaseHistory.add(new PhaseRecord(oldPhase, Instant.now()));
            }
            currentPhase.set(phase);
            currentPhaseTiming = new PhaseTiming(phase);
            notifyListeners();
        }
    }

    public synchronized void setPhase(GenerationPhase phase, String message) {
        this.currentMessage = message;
        setPhase(phase);
    }

    public synchronized void setPhase(GenerationPhase phase, String message, String detail) {
        this.currentMessage = message;
        this.currentDetail = detail;
        setPhase(phase);
    }

    public void incrementIteration() {
        currentIteration.incrementAndGet();
        notifyListeners();
    }

    public void setIteration(int iteration) {
        currentIteration.set(iteration);
        notifyListeners();
    }

    public void setMaxIterations(int max) {
        maxIterations.set(max);
        notifyListeners();
    }

    public void incrementFilesProcessed() {
        filesProcessed.incrementAndGet();
        notifyListeners();
    }

    public void setFilesProcessed(int count) {
        filesProcessed.set(count);
        notifyListeners();
    }

    public void setTotalFiles(int total) {
        totalFiles.set(total);
        notifyListeners();
    }

    public void addTokenUsage(TokenUsage usage) {
        if (usage != null) {
            promptTokens.addAndGet(usage.promptTokens());
            completionTokens.addAndGet(usage.completionTokens());
            totalTokens.addAndGet(usage.totalTokens());
            notifyListeners();
        }
    }

    public void incrementLlmCalls() {
        llmCalls.incrementAndGet();
        notifyListeners();
    }
    
    public void incrementTestMethods(int count) {
        testMethodsGenerated.addAndGet(count);
        notifyListeners();
    }
    
    public void incrementTestClasses() {
        testClassesGenerated.incrementAndGet();
        notifyListeners();
    }

    public void setCoverage(double lineCoverage, double branchCoverage) {
        this.currentCoverage.set(lineCoverage);
        this.branchCoverage.set(branchCoverage);
        coverageTrend.addPoint(lineCoverage, branchCoverage);
        notifyListeners();
    }

    public void setCoverage(CoverageReport report) {
        if (report != null) {
            this.currentCoverage.set(report.overallLineCoverage());
            this.branchCoverage.set(report.overallBranchCoverage());
            notifyListeners();
        }
    }

    public void setTargetCoverage(double target) {
        targetCoverage.set(target);
        notifyListeners();
    }

    public void setMessage(String message) {
        this.currentMessage = message;
        notifyListeners();
    }

    public void setDetail(String detail) {
        this.currentDetail = detail;
        notifyListeners();
    }

    public void addMessage(String message) {
        synchronized (messages) {
            messages.add(message);
        }
        notifyListeners();
    }

    public void setError(Throwable error) {
        this.lastError = error;
        setPhase(GenerationPhase.FAILED, error.getMessage());
    }

    public void complete() {
        setPhase(GenerationPhase.COMPLETED);
    }

    public GenerationPhase getCurrentPhase() {
        return currentPhase.get();
    }

    public int getCurrentIteration() {
        return currentIteration.get();
    }

    public int getMaxIterations() {
        return maxIterations.get();
    }

    public int getFilesProcessed() {
        return filesProcessed.get();
    }

    public int getTotalFiles() {
        return totalFiles.get();
    }

    public long getTotalTokens() {
        return totalTokens.get();
    }

    public long getPromptTokens() {
        return promptTokens.get();
    }

    public long getCompletionTokens() {
        return completionTokens.get();
    }

    public int getLlmCalls() {
        return llmCalls.get();
    }

    public double getCurrentCoverage() {
        return currentCoverage.get();
    }

    public double getBranchCoverage() {
        return branchCoverage.get();
    }

    public double getTargetCoverage() {
        return targetCoverage.get();
    }

    public String getCurrentMessage() {
        return currentMessage;
    }

    public String getCurrentDetail() {
        return currentDetail;
    }

    public Throwable getLastError() {
        return lastError;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getClassName() {
        return className;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Duration getElapsedDuration() {
        return Duration.between(startTime, Instant.now());
    }

    public long getElapsedMs() {
        return getElapsedDuration().toMillis();
    }

    public double getOverallProgress() {
        GenerationPhase phase = currentPhase.get();
        if (phase.isTerminal()) {
            return 1.0;
        }
        return phase.getProgressPercentage() / 100.0;
    }

    public double getIterationProgress() {
        int max = maxIterations.get();
        if (max <= 0) return 0;
        return (double) currentIteration.get() / max;
    }

    public double getFileProgress() {
        int total = totalFiles.get();
        if (total <= 0) return 0;
        return (double) filesProcessed.get() / total;
    }

    public double getEstimatedCost(double promptPrice, double completionPrice) {
        return (promptTokens.get() * promptPrice / 1000.0) +
               (completionTokens.get() * completionPrice / 1000.0);
    }

    public double getEstimatedCostDefault() {
        return getEstimatedCost(0.01, 0.03);
    }

    public String getFormattedDuration() {
        long ms = getElapsedMs();
        if (ms < 1000) {
            return ms + "ms";
        } else if (ms < 60000) {
            return String.format("%.1fs", ms / 1000.0);
        } else {
            long minutes = ms / 60000;
            long seconds = (ms % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    public double getTokensPerSecond() {
        long seconds = getElapsedDuration().toSeconds();
        if (seconds <= 0) return 0;
        return (double) totalTokens.get() / seconds;
    }
    
    public double getTestsPerMinute() {
        long minutes = getElapsedDuration().toMinutes();
        if (minutes <= 0) {
            long seconds = getElapsedDuration().toSeconds();
            if (seconds <= 0) return 0;
            return (double) testMethodsGenerated.get() / seconds * 60;
        }
        return (double) testMethodsGenerated.get() / minutes;
    }

    public boolean isComplete() {
        return currentPhase.get() == GenerationPhase.COMPLETED;
    }

    public boolean isFailed() {
        return currentPhase.get() == GenerationPhase.FAILED;
    }

    public boolean isTerminal() {
        return currentPhase.get().isTerminal();
    }

    public List<String> getMessages() {
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }
    
    public Map<GenerationPhase, PhaseTiming> getPhaseTimings() {
        return new HashMap<>(phaseTimings);
    }
    
    public PhaseTiming getCurrentPhaseTiming() {
        return currentPhaseTiming;
    }
    
    public CoverageTrend getCoverageTrend() {
        return coverageTrend;
    }
    
    public int getTestMethodsGenerated() {
        return testMethodsGenerated.get();
    }
    
    public int getTestClassesGenerated() {
        return testClassesGenerated.get();
    }
    
    public long getTotalPhaseDurationMs() {
        return phaseTimings.values().stream()
            .mapToLong(PhaseTiming::getDurationMs)
            .sum();
    }
    
    public String getEstimatedRemainingTime() {
        if (currentIteration.get() == 0 || maxIterations.get() <= 0) {
            return "Calculating...";
        }
        
        long elapsedMs = getElapsedMs();
        double progress = (double) currentIteration.get() / maxIterations.get();
        
        if (progress <= 0) {
            return "Calculating...";
        }
        
        long estimatedTotalMs = (long) (elapsedMs / progress);
        long remainingMs = estimatedTotalMs - elapsedMs;
        
        if (remainingMs < 0) {
            return "Almost done";
        }
        
        if (remainingMs < 1000) {
            return "< 1s";
        } else if (remainingMs < 60000) {
            return String.format("~%.0fs", remainingMs / 1000.0);
        } else {
            long minutes = remainingMs / 60000;
            long seconds = (remainingMs % 60000) / 1000;
            return String.format("~%dm %ds", minutes, seconds);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(
            sourceFile, className, currentPhase.get(), currentIteration.get(),
            maxIterations.get(), filesProcessed.get(), totalFiles.get(),
            totalTokens.get(), promptTokens.get(), completionTokens.get(),
            llmCalls.get(), currentCoverage.get(), branchCoverage.get(),
            targetCoverage.get(), currentMessage, currentDetail,
            getElapsedMs(), lastError, testMethodsGenerated.get(), testClassesGenerated.get()
        );
    }

    public record Snapshot(
        String sourceFile,
        String className,
        GenerationPhase phase,
        int iteration,
        int maxIterations,
        int filesProcessed,
        int totalFiles,
        long totalTokens,
        long promptTokens,
        long completionTokens,
        int llmCalls,
        double coverage,
        double branchCoverage,
        double targetCoverage,
        String message,
        String detail,
        long elapsedMs,
        Throwable error,
        int testMethodsGenerated,
        int testClassesGenerated
    ) {}

    private record PhaseRecord(GenerationPhase phase, Instant endTime) {}
}
