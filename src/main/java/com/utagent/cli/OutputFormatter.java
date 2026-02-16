package com.utagent.cli;

import com.utagent.config.OutputConfig;
import com.utagent.model.CoverageReport;
import com.utagent.parser.FrameworkType;
import com.utagent.optimizer.OptimizationResult;

import java.util.List;
import java.util.Set;

/**
 * Output formatter for CLI tool. Handles all user-facing output formatting.
 */
public class OutputFormatter {

    private final OutputConfig outputConfig;

    public OutputFormatter(OutputConfig outputConfig) {
        this.outputConfig = outputConfig;
    }

    /**
     * Print the tool banner and usage information.
     */
    public void printBanner() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           Java UT Agent - AI Test Generator                ║");
        System.out.println("║                    Version 1.0.0                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Usage: java-ut-agent <source> [options]");
        System.out.println();
        System.out.println("LLM Providers:");
        System.out.println("  openai    - OpenAI GPT models (default)");
        System.out.println("  claude    - Anthropic Claude models");
        System.out.println("  ollama    - Ollama local models");
        System.out.println("  deepseek  - DeepSeek models");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-ut-agent src/main/java/com/example/MyService.java");
        System.out.println("  java-ut-agent src/main/java -t 0.9 -p claude -a $ANTHROPIC_API_KEY");
        System.out.println("  java-ut-agent MyClass.java --dry-run --stream");
        System.out.println("  java-ut-agent --init");
        System.out.println();
        System.out.println("Run with --help for all options.");
    }

    /**
     * Print test generation start message.
     */
    public void printGenerationStart() {
        System.out.println("🚀 Java UT Agent - Starting test generation...");
        System.out.println();
    }

    /**
     * Print LLM provider information.
     */
    public void printProviderInfo(String providerName, String modelName) {
        if (modelName != null) {
            System.out.println("✨ Using " + providerName + " provider");
            System.out.println("📝 Model: " + modelName);
        } else {
            System.out.println("📝 Using template-based generation (set --api-key for AI-powered generation)");
        }
    }

    /**
     * Print coverage target information.
     */
    public void printTargetInfo(double targetCoverage, int maxIterations) {
        System.out.println("🎯 Target coverage: " + String.format("%.0f%%", targetCoverage * 100));
        System.out.println("🔄 Max iterations: " + maxIterations);
        System.out.println();
    }

    /**
     * Print dry run mode message.
     */
    public void printDryRunMessage() {
        System.out.println("📋 Dry run mode - generating tests without execution");
        System.out.println();
    }

    /**
     * Print coverage analysis start message.
     */
    public void printCoverageAnalysisStart() {
        System.out.println("📊 Analyzing coverage...");
        System.out.println();
    }

    /**
     * Print framework detection start message.
     */
    public void printFrameworkDetectionStart() {
        System.out.println("🔍 Detecting frameworks...");
        System.out.println();
    }

    /**
     * Print framework detection results.
     */
    public void printFrameworks(String className, Set<FrameworkType> frameworks) {
        System.out.println("📦 " + (className != null ? className : "Unknown class"));
        if (frameworks == null || frameworks.isEmpty()) {
            System.out.println("   No frameworks detected");
        } else {
            for (FrameworkType framework : frameworks) {
                System.out.println("   ✓ " + framework.getDisplayName());
            }
        }
        System.out.println();
    }

    /**
     * Print progress message.
     */
    public void printProgress(String message) {
        if (outputConfig.getVerboseOrDefault()) {
            System.out.println("[INFO] " + message);
        }
    }

    /**
     * Print coverage update message.
     */
    public void printCoverage(CoverageReport report) {
        System.out.println("📈 Coverage update:");
        System.out.println("   Line: " + String.format("%.1f%%", report.overallLineCoverage() * 100));
        System.out.println("   Branch: " + String.format("%.1f%%", report.overallBranchCoverage() * 100));
        System.out.println();
    }

    /**
     * Print optimization result.
     */
    public void printResult(OptimizationResult result) {
        System.out.println();
        System.out.println(result.getSummary());
        
        if (result.isSuccess()) {
            System.out.println("🎉 Target coverage achieved!");
        } else {
            System.out.println("⚠️ Target coverage not reached. Consider:");
            System.out.println("   - Increasing max iterations");
            System.out.println("   - Using AI-powered generation with --api-key");
            System.out.println("   - Trying a different LLM provider with --provider");
            System.out.println("   - Manually reviewing uncovered code paths");
        }
    }

    /**
     * Print optimization summary for multiple files.
     */
    public void printSummary(List<OptimizationResult> results) {
        System.out.println();
        System.out.println("=== Summary ===");
        System.out.println("Processed " + results.size() + " files");
        
        long successCount = results.stream().filter(OptimizationResult::isSuccess).count();
        System.out.println("Target achieved: " + successCount + "/" + results.size());
        
        double avgImprovement = results.stream()
            .mapToDouble(OptimizationResult::getCoverageImprovement)
            .average()
            .orElse(0.0);
        System.out.println("Average coverage improvement: " + String.format("%.1f%%", avgImprovement * 100));
    }

    /**
     * Print configuration initialization success message.
     */
    public void printConfigInitSuccess(String configPath) {
        System.out.println("✅ Created default configuration file: " + configPath);
        System.out.println("⚠️  Please edit the configuration file and add your API key.");
    }

    /**
     * Print configuration initialization failure message.
     */
    public void printConfigInitFailure(String errorMessage) {
        System.err.println("Failed to create configuration file: " + errorMessage);
    }

    /**
     * Print source path not found error.
     */
    public void printSourceNotFoundError(String path) {
        System.err.println("Source path does not exist: " + path);
    }

    /**
     * Print coverage data not found error.
     */
    public void printCoverageDataNotFoundError() {
        System.err.println("No coverage data found");
        System.err.println("Run tests with JaCoCo first");
    }

    /**
     * Print coverage target status.
     */
    public void printCoverageTargetStatus(double target, boolean meetsTarget) {
        if (meetsTarget) {
            System.out.println("✅ Coverage meets target: " + String.format("%.0f%%", target * 100));
        } else {
            System.out.println("❌ Coverage below target: " + String.format("%.0f%%", target * 100));
        }
    }
}
