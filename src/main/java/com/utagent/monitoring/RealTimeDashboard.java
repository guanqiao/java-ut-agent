package com.utagent.monitoring;

import com.utagent.terminal.AnsiColor;
import com.utagent.terminal.ProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RealTimeDashboard {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeDashboard.class);
    
    private static final int UPDATE_INTERVAL_MS = 100;
    private static final int DASHBOARD_HEIGHT = 12;
    
    private final PrintStream out;
    private final boolean colorEnabled;
    private final AtomicReference<GenerationProgress> progressRef;
    private final AtomicBoolean active;
    private final ScheduledExecutorService scheduler;
    
    private final ProgressBar progressBar;
    private final LLMCallMonitor llmMonitor;
    
    private long lastUpdateTime = 0;
    private String lastRendered = "";

    public RealTimeDashboard() {
        this(System.out);
    }

    public RealTimeDashboard(PrintStream out) {
        this.out = out;
        this.colorEnabled = AnsiColor.isColorSupported();
        this.progressRef = new AtomicReference<>();
        this.active = new AtomicBoolean(false);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dashboard-renderer");
            t.setDaemon(true);
            return t;
        });
        this.progressBar = new ProgressBar(out, 30, true);
        this.llmMonitor = LLMCallMonitor.getInstance();
    }

    public void start(GenerationProgress progress) {
        progressRef.set(progress);
        active.set(true);
        
        clearScreen();
        printHeader();
        
        scheduler.scheduleAtFixedRate(
            this::render,
            0,
            UPDATE_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        
        logger.debug("Dashboard started");
    }

    public void stop() {
        active.set(false);
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        renderFinal();
        logger.debug("Dashboard stopped");
    }

    public void update() {
        if (active.get()) {
            render();
        }
    }

    private void render() {
        GenerationProgress progress = progressRef.get();
        if (progress == null) return;
        
        if (progress.isTerminal()) {
            renderFinal();
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        
        sb.append(moveToLine(3));
        
        renderPhase(sb, progress);
        renderProgressBars(sb, progress);
        renderCoverageWithTrend(sb, progress);
        renderLLMStats(sb, progress);
        renderPerformanceMetrics(sb, progress);
        renderCurrentMessage(sb, progress);
        
        String content = sb.toString();
        if (!content.equals(lastRendered)) {
            out.print(content);
            lastRendered = content;
        }
    }

    private void renderFinal() {
        GenerationProgress progress = progressRef.get();
        if (progress == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append(moveToLine(3));
        
        renderPhase(sb, progress);
        renderProgressBars(sb, progress);
        renderCoverageWithTrend(sb, progress);
        renderLLMStats(sb, progress);
        renderPerformanceMetrics(sb, progress);
        renderPhaseTimingSummary(sb, progress);
        renderFinalStatus(sb, progress);
        
        out.print(sb.toString());
        out.println();
    }

    private void printHeader() {
        out.print(AnsiColor.CURSOR_SAVE);
        out.print(AnsiColor.CURSOR_HIDE);
        
        out.println();
        if (colorEnabled) {
            out.println(AnsiColor.colorize("╔══════════════════════════════════════════════════════════════╗", AnsiColor.CYAN));
            out.println(AnsiColor.colorize("║              ", AnsiColor.CYAN) + 
                       AnsiColor.colorize("Java UT Agent - Generation Dashboard", AnsiColor.BOLD, AnsiColor.WHITE) +
                       AnsiColor.colorize("              ║", AnsiColor.CYAN));
            out.println(AnsiColor.colorize("╚══════════════════════════════════════════════════════════════╝", AnsiColor.CYAN));
        } else {
            out.println("================================================================");
            out.println("              Java UT Agent - Generation Dashboard");
            out.println("================================================================");
        }
        out.println();
    }

    private void renderPhase(StringBuilder sb, GenerationProgress progress) {
        GenerationPhase phase = progress.getCurrentPhase();
        String phaseLine = String.format("  %-20s %s %s", 
            "Phase:", 
            phase.getIcon(), 
            phase.getDisplayName());
        
        if (colorEnabled) {
            AnsiColor color = phase.isTerminal() 
                ? (phase == GenerationPhase.COMPLETED ? AnsiColor.GREEN : AnsiColor.RED)
                : AnsiColor.CYAN;
            sb.append(AnsiColor.colorize(phaseLine, color)).append("\n");
        } else {
            sb.append(phaseLine).append("\n");
        }
    }

    private void renderProgressBars(StringBuilder sb, GenerationProgress progress) {
        sb.append("\n");
        
        String overallBar = buildProgressBar(progress.getOverallProgress(), 20);
        String overallLine = String.format("  %-20s %s %5.1f%%", 
            "Overall Progress:", overallBar, progress.getOverallProgress() * 100);
        sb.append(colorizeIfEnabled(overallLine, AnsiColor.WHITE)).append("\n");
        
        if (progress.getMaxIterations() > 1) {
            String iterBar = buildProgressBar(progress.getIterationProgress(), 20);
            String iterLine = String.format("  %-20s %s %d/%d iterations", 
                "Iteration:", iterBar, progress.getCurrentIteration(), progress.getMaxIterations());
            sb.append(colorizeIfEnabled(iterLine, AnsiColor.YELLOW)).append("\n");
        }
        
        if (progress.getTotalFiles() > 1) {
            String fileBar = buildProgressBar(progress.getFileProgress(), 20);
            String fileLine = String.format("  %-20s %s %d/%d files", 
                "Files:", fileBar, progress.getFilesProcessed(), progress.getTotalFiles());
            sb.append(colorizeIfEnabled(fileLine, AnsiColor.YELLOW)).append("\n");
        }
    }

    private void renderCoverage(StringBuilder sb, GenerationProgress progress) {
        sb.append("\n");
        
        double lineCov = progress.getCurrentCoverage();
        double branchCov = progress.getBranchCoverage();
        double targetCov = progress.getTargetCoverage();
        
        String lineBar = buildCoverageBar(lineCov, 15);
        String branchBar = buildCoverageBar(branchCov, 15);
        
        String lineCovStr = String.format("  %-20s %s %5.1f%% (target: %.0f%%)", 
            "Line Coverage:", lineBar, lineCov * 100, targetCov * 100);
        sb.append(colorizeCoverage(lineCovStr, lineCov, targetCov)).append("\n");
        
        String branchCovStr = String.format("  %-20s %s %5.1f%%", 
            "Branch Coverage:", branchBar, branchCov * 100);
        sb.append(colorizeCoverage(branchCovStr, branchCov, targetCov)).append("\n");
    }

    private void renderCoverageWithTrend(StringBuilder sb, GenerationProgress progress) {
        sb.append("\n");
        
        double lineCov = progress.getCurrentCoverage();
        double branchCov = progress.getBranchCoverage();
        double targetCov = progress.getTargetCoverage();
        
        String lineBar = buildCoverageBar(lineCov, 15);
        String branchBar = buildCoverageBar(branchCov, 15);
        
        String lineCovStr = String.format("  %-20s %s %5.1f%% (target: %.0f%%)", 
            "Line Coverage:", lineBar, lineCov * 100, targetCov * 100);
        sb.append(colorizeCoverage(lineCovStr, lineCov, targetCov)).append("\n");
        
        String branchCovStr = String.format("  %-20s %s %5.1f%%", 
            "Branch Coverage:", branchBar, branchCov * 100);
        sb.append(colorizeCoverage(branchCovStr, branchCov, targetCov)).append("\n");
    }
    
    private void renderLLMStats(StringBuilder sb, GenerationProgress progress) {
        sb.append("\n");
        
        LLMCallMonitor.Statistics stats = llmMonitor.getStatistics();
        
        String tokenLine = String.format("  %-20s %s (prompt: %s, completion: %s)",
            "Tokens:",
            formatNumber(progress.getTotalTokens()),
            formatNumber(progress.getPromptTokens()),
            formatNumber(progress.getCompletionTokens()));
        sb.append(colorizeIfEnabled(tokenLine, AnsiColor.MAGENTA)).append("\n");
        
        String callLine = String.format("  %-20s %d calls (%d success, %d failed) avg %.0fms",
            "LLM Calls:",
            stats.totalCalls(),
            stats.successfulCalls(),
            stats.failedCalls(),
            stats.averageLatencyMs());
        sb.append(colorizeIfEnabled(callLine, AnsiColor.MAGENTA)).append("\n");
        
        String costLine = String.format("  %-20s $%.4f",
            "Estimated Cost:",
            progress.getEstimatedCostDefault());
        sb.append(colorizeIfEnabled(costLine, AnsiColor.GREEN)).append("\n");
        
        String timeLine = String.format("  %-20s %s",
            "Elapsed Time:",
            progress.getFormattedDuration());
        sb.append(colorizeIfEnabled(timeLine, AnsiColor.WHITE)).append("\n");
        
        String etaLine = String.format("  %-20s %s",
            "ETA:",
            progress.getEstimatedRemainingTime());
        sb.append(colorizeIfEnabled(etaLine, AnsiColor.CYAN)).append("\n");
    }
    
    private void renderPerformanceMetrics(StringBuilder sb, GenerationProgress progress) {
        sb.append("\n");
        
        String testLine = String.format("  %-20s %d methods in %d classes",
            "Tests Generated:",
            progress.getTestMethodsGenerated(),
            progress.getTestClassesGenerated());
        sb.append(colorizeIfEnabled(testLine, AnsiColor.WHITE)).append("\n");
        
        double tps = progress.getTokensPerSecond();
        String tpsLine = String.format("  %-20s %.1f tokens/sec",
            "Token Rate:",
            tps);
        sb.append(colorizeIfEnabled(tpsLine, AnsiColor.MAGENTA)).append("\n");
        
        double testsPerMin = progress.getTestsPerMinute();
        if (testsPerMin > 0) {
            String testRateLine = String.format("  %-20s %.1f tests/min",
                "Test Rate:",
                testsPerMin);
            sb.append(colorizeIfEnabled(testRateLine, AnsiColor.CYAN)).append("\n");
        }
    }
    
    private void renderPhaseTimingSummary(StringBuilder sb, GenerationProgress progress) {
        sb.append("\n");
        sb.append(colorizeIfEnabled("  === Phase Timing Summary ===", AnsiColor.CYAN)).append("\n");
        
        var timings = progress.getPhaseTimings();
        long totalMs = progress.getTotalPhaseDurationMs();
        
        timings.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue().getDurationMs(), a.getValue().getDurationMs()))
            .limit(5)
            .forEach(entry -> {
                GenerationPhase phase = entry.getKey();
                PhaseTiming timing = entry.getValue();
                String timingLine = String.format("  %-20s %s (%.1f%%)",
                    phase.getDisplayName() + ":",
                    timing.getFormattedDuration(),
                    timing.getPercentageOfTotal(totalMs));
                sb.append(colorizeIfEnabled(timingLine, AnsiColor.DIM)).append("\n");
            });
    }

    private void renderCurrentMessage(StringBuilder sb, GenerationProgress progress) {
        String message = progress.getCurrentMessage();
        if (message != null && !message.isEmpty()) {
            sb.append("\n");
            String msgLine = String.format("  %-20s %s", "Status:", truncate(message, 50));
            sb.append(colorizeIfEnabled(msgLine, AnsiColor.DIM)).append("\n");
        }
        
        String detail = progress.getCurrentDetail();
        if (detail != null && !detail.isEmpty()) {
            String detailLine = String.format("  %-20s %s", "Detail:", truncate(detail, 50));
            sb.append(colorizeIfEnabled(detailLine, AnsiColor.DIM)).append("\n");
        }
    }

    private void renderFinalStatus(StringBuilder sb, GenerationProgress progress) {
        sb.append("\n");
        
        if (progress.isComplete()) {
            String successLine = "  ✅ Generation completed successfully!";
            sb.append(colorizeIfEnabled(successLine, AnsiColor.GREEN)).append("\n");
        } else if (progress.isFailed()) {
            String failLine = "  ❌ Generation failed: " + 
                (progress.getLastError() != null ? progress.getLastError().getMessage() : "Unknown error");
            sb.append(colorizeIfEnabled(failLine, AnsiColor.RED)).append("\n");
        }
        
        out.print(AnsiColor.CURSOR_SHOW);
        out.print(AnsiColor.CURSOR_RESTORE);
    }

    private String buildProgressBar(double progress, int width) {
        int filled = (int) (progress * width);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");
        return bar.toString();
    }

    private String buildCoverageBar(double coverage, int width) {
        int filled = (int) (coverage * width);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");
        return bar.toString();
    }

    private String colorizeIfEnabled(String text, AnsiColor color) {
        if (colorEnabled) {
            return AnsiColor.colorize(text, color);
        }
        return text;
    }

    private String colorizeCoverage(String text, double coverage, double target) {
        if (!colorEnabled) return text;
        
        AnsiColor color;
        if (coverage >= target) {
            color = AnsiColor.GREEN;
        } else if (coverage >= target * 0.7) {
            color = AnsiColor.YELLOW;
        } else {
            color = AnsiColor.RED;
        }
        return AnsiColor.colorize(text, color);
    }

    private String formatNumber(long num) {
        if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.1fK", num / 1_000.0);
        }
        return String.valueOf(num);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }

    private String moveToLine(int line) {
        return "\u001B[" + line + ";1H";
    }

    private void clearScreen() {
        out.print("\u001B[2J");
        out.print("\u001B[H");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PrintStream out = System.out;

        public Builder out(PrintStream out) {
            this.out = out;
            return this;
        }

        public RealTimeDashboard build() {
            return new RealTimeDashboard(out);
        }
    }
}
