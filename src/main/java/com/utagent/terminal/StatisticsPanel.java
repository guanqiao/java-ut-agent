package com.utagent.terminal;

import java.io.PrintStream;
import java.util.List;

public class StatisticsPanel {

    private final PrintStream out;
    private final boolean colorEnabled;
    
    public StatisticsPanel() {
        this(System.out);
    }
    
    public StatisticsPanel(PrintStream out) {
        this.out = out;
        this.colorEnabled = AnsiColor.isColorSupported();
    }
    
    public void printStatistics(Statistics stats) {
        printSeparator();
        
        printRow("Files Processed", String.valueOf(stats.filesProcessed()));
        printRow("Tests Generated", String.valueOf(stats.testsGenerated()));
        printRow("Tests Passed", formatCount(stats.testsPassed(), stats.testsGenerated() - stats.testsFailed()));
        printRow("Tests Failed", formatCount(stats.testsFailed(), stats.testsFailed()));
        
        printSeparator();
        
        printCoverageRow("Line Coverage", stats.lineCoverage());
        printCoverageRow("Branch Coverage", stats.branchCoverage());
        printCoverageRow("Instruction Coverage", stats.instructionCoverage());
        
        printSeparator();
        
        if (stats.tokenUsage() != null) {
            printRow("Tokens Used", formatTokens(stats.tokenUsage().totalTokens()));
            if (stats.estimatedCost() > 0) {
                printRow("Estimated Cost", String.format("$%.4f", stats.estimatedCost()));
            }
        }
        
        printRow("Duration", formatDuration(stats.durationMs()));
        
        printSeparator();
    }
    
    private void printSeparator() {
        out.println(colorEnabled ? 
            AnsiColor.colorize("├" + "─".repeat(50) + "┤", AnsiColor.DIM) :
            "+" + "-".repeat(50) + "+");
    }
    
    private void printRow(String label, String value) {
        String line = String.format("│ %-20s: %-27s │", label, value);
        if (colorEnabled) {
            out.println(AnsiColor.colorize(line, AnsiColor.DIM));
        } else {
            out.println(line);
        }
    }
    
    private void printCoverageRow(String label, double coverage) {
        String bar = buildCoverageBar(coverage);
        String value = String.format("%5.1f%% %s", coverage * 100, bar);
        String line = String.format("│ %-20s: %-27s │", label, value);
        
        if (colorEnabled) {
            AnsiColor color = coverage >= 0.8 ? AnsiColor.GREEN : 
                             coverage >= 0.5 ? AnsiColor.YELLOW : AnsiColor.RED;
            out.println(AnsiColor.colorize(line, color));
        } else {
            out.println(line);
        }
    }
    
    private String buildCoverageBar(double coverage) {
        int filled = (int) (coverage * 10);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? "█" : "░");
        }
        bar.append("]");
        return bar.toString();
    }
    
    private String formatCount(int count, int value) {
        if (value > 0 && colorEnabled) {
            return AnsiColor.colorize(String.valueOf(value), 
                count > 0 ? AnsiColor.RED : AnsiColor.GREEN);
        }
        return String.valueOf(value);
    }
    
    private String formatTokens(int tokens) {
        if (tokens >= 1_000_000) {
            return String.format("%.1fM", tokens / 1_000_000.0);
        } else if (tokens >= 1_000) {
            return String.format("%.1fK", tokens / 1_000.0);
        }
        return String.valueOf(tokens);
    }
    
    private String formatDuration(long ms) {
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
    
    public record Statistics(
        int filesProcessed,
        int testsGenerated,
        int testsPassed,
        int testsFailed,
        double lineCoverage,
        double branchCoverage,
        double instructionCoverage,
        com.utagent.llm.TokenUsage tokenUsage,
        double estimatedCost,
        long durationMs
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private int filesProcessed;
            private int testsGenerated;
            private int testsPassed;
            private int testsFailed;
            private double lineCoverage;
            private double branchCoverage;
            private double instructionCoverage;
            private com.utagent.llm.TokenUsage tokenUsage;
            private double estimatedCost;
            private long durationMs;
            
            public Builder filesProcessed(int filesProcessed) {
                this.filesProcessed = filesProcessed;
                return this;
            }
            
            public Builder testsGenerated(int testsGenerated) {
                this.testsGenerated = testsGenerated;
                return this;
            }
            
            public Builder testsPassed(int testsPassed) {
                this.testsPassed = testsPassed;
                return this;
            }
            
            public Builder testsFailed(int testsFailed) {
                this.testsFailed = testsFailed;
                return this;
            }
            
            public Builder lineCoverage(double lineCoverage) {
                this.lineCoverage = lineCoverage;
                return this;
            }
            
            public Builder branchCoverage(double branchCoverage) {
                this.branchCoverage = branchCoverage;
                return this;
            }
            
            public Builder instructionCoverage(double instructionCoverage) {
                this.instructionCoverage = instructionCoverage;
                return this;
            }
            
            public Builder tokenUsage(com.utagent.llm.TokenUsage tokenUsage) {
                this.tokenUsage = tokenUsage;
                return this;
            }
            
            public Builder estimatedCost(double estimatedCost) {
                this.estimatedCost = estimatedCost;
                return this;
            }
            
            public Builder durationMs(long durationMs) {
                this.durationMs = durationMs;
                return this;
            }
            
            public Statistics build() {
                return new Statistics(
                    filesProcessed, testsGenerated, testsPassed, testsFailed,
                    lineCoverage, branchCoverage, instructionCoverage,
                    tokenUsage, estimatedCost, durationMs
                );
            }
        }
    }
}
