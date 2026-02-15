package com.utagent.cli;

import com.utagent.coverage.CoverageAnalyzer;
import com.utagent.generator.TestGenerator;
import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageReport;
import com.utagent.optimizer.IterativeOptimizer;
import com.utagent.optimizer.OptimizationResult;
import com.utagent.parser.FrameworkDetector;
import com.utagent.parser.FrameworkType;
import com.utagent.parser.JavaCodeParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(
    name = "java-ut-agent",
    description = "AI-powered JUnit 5 unit test generator with coverage optimization",
    mixinStandardHelpOptions = true,
    version = "1.0.0"
)
public class CLICommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Source file or directory to generate tests for", arity = "0..1")
    private File source;

    @Option(names = {"-t", "--target"}, description = "Target coverage rate (0.0-1.0, default: 0.8)")
    private double targetCoverage = 0.8;

    @Option(names = {"-i", "--iterations"}, description = "Maximum iterations for optimization (default: 10)")
    private int maxIterations = 10;

    @Option(names = {"-a", "--api-key"}, description = "OpenAI API key for AI-powered generation")
    private String apiKey;

    @Option(names = {"--api-url"}, description = "Custom API URL (for alternative LLM providers)")
    private String apiUrl;

    @Option(names = {"--model"}, description = "LLM model to use (default: gpt-4)")
    private String model = "gpt-4";

    @Option(names = {"-o", "--output"}, description = "Output directory for generated tests")
    private File outputDir;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose = false;

    @Option(names = {"--dry-run"}, description = "Generate tests without running them")
    private boolean dryRun = false;

    @Option(names = {"--analyze-only"}, description = "Only analyze coverage, don't generate tests")
    private boolean analyzeOnly = false;

    @Option(names = {"--detect-framework"}, description = "Detect frameworks used in the source code")
    private boolean detectFramework = false;

    @Override
    public Integer call() throws Exception {
        if (source == null) {
            printBanner();
            return 0;
        }

        if (!source.exists()) {
            System.err.println("Error: Source path does not exist: " + source.getAbsolutePath());
            return 1;
        }

        if (analyzeOnly) {
            return analyzeCoverage();
        }

        if (detectFramework) {
            return detectFrameworks();
        }

        return generateTests();
    }

    private void printBanner() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           Java UT Agent - AI Test Generator                ║");
        System.out.println("║                    Version 1.0.0                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Usage: java-ut-agent <source> [options]");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-ut-agent src/main/java/com/example/MyService.java");
        System.out.println("  java-ut-agent src/main/java -t 0.9 -a $OPENAI_API_KEY");
        System.out.println("  java-ut-agent MyClass.java --dry-run");
        System.out.println();
        System.out.println("Run with --help for all options.");
    }

    private int generateTests() {
        System.out.println("🚀 Java UT Agent - Starting test generation...");
        System.out.println();
        
        if (apiKey != null) {
            System.out.println("✨ AI-powered generation enabled");
        } else {
            System.out.println("📝 Using template-based generation (set --api-key for AI-powered generation)");
        }
        
        System.out.println("🎯 Target coverage: " + String.format("%.0f%%", targetCoverage * 100));
        System.out.println("🔄 Max iterations: " + maxIterations);
        System.out.println();

        IterativeOptimizer optimizer = new IterativeOptimizer(getProjectRoot(), apiKey)
            .setTargetCoverage(targetCoverage)
            .setMaxIterations(maxIterations)
            .setVerbose(verbose)
            .setProgressListener(this::printProgress)
            .setCoverageListener(this::printCoverage);

        if (dryRun) {
            return generateTestsDryRun();
        }

        OptimizationResult result;
        if (source.isFile()) {
            result = optimizer.optimize(source);
            printResult(result);
        } else {
            List<OptimizationResult> results = optimizer.optimizeDirectory(source);
            printSummary(results);
        }

        return 0;
    }

    private int generateTestsDryRun() {
        System.out.println("📋 Dry run mode - generating tests without execution");
        System.out.println();

        JavaCodeParser parser = new JavaCodeParser();
        TestGenerator generator = new TestGenerator(apiKey);

        if (source.isFile()) {
            var classInfo = parser.parseFile(source);
            if (classInfo.isPresent()) {
                String testCode = generator.generateTestClass(classInfo.get());
                System.out.println("Generated test for: " + classInfo.get().className());
                System.out.println();
                System.out.println(testCode);
            }
        } else {
            List<ClassInfo> classes = parser.parseDirectory(source);
            System.out.println("Found " + classes.size() + " classes to generate tests for");
            
            for (ClassInfo classInfo : classes) {
                System.out.println("  - " + classInfo.fullyQualifiedName());
            }
        }

        return 0;
    }

    private int analyzeCoverage() {
        System.out.println("📊 Analyzing coverage...");
        System.out.println();

        File projectRoot = getProjectRoot();
        CoverageAnalyzer analyzer = new CoverageAnalyzer(projectRoot);

        File jacocoXml = new File(projectRoot, "target/site/jacoco/jacoco.xml");
        File execFile = new File(projectRoot, "target/jacoco.exec");

        CoverageReport report;
        if (jacocoXml.exists()) {
            report = analyzer.analyzeFromJacocoXml(jacocoXml);
        } else if (execFile.exists()) {
            report = analyzer.analyzeCoverage(execFile);
        } else {
            System.err.println("No coverage data found. Run tests with JaCoCo first.");
            return 1;
        }

        System.out.println(analyzer.formatCoverageReport(report));
        
        if (report.meetsTarget(targetCoverage)) {
            System.out.println("✅ Coverage meets target: " + String.format("%.0f%%", targetCoverage * 100));
        } else {
            System.out.println("❌ Coverage below target: " + String.format("%.0f%%", targetCoverage * 100));
        }

        return 0;
    }

    private int detectFrameworks() {
        System.out.println("🔍 Detecting frameworks...");
        System.out.println();

        JavaCodeParser parser = new JavaCodeParser();
        FrameworkDetector detector = new FrameworkDetector();

        if (source.isFile()) {
            var classInfo = parser.parseFile(source);
            if (classInfo.isPresent()) {
                Set<FrameworkType> frameworks = detector.detectFrameworks(classInfo.get());
                printFrameworks(classInfo.get().className(), frameworks);
            }
        } else {
            List<ClassInfo> classes = parser.parseDirectory(source);
            for (ClassInfo classInfo : classes) {
                Set<FrameworkType> frameworks = detector.detectFrameworks(classInfo);
                printFrameworks(classInfo.fullyQualifiedName(), frameworks);
            }
        }

        return 0;
    }

    private void printFrameworks(String className, Set<FrameworkType> frameworks) {
        System.out.println("📦 " + className);
        if (frameworks.isEmpty()) {
            System.out.println("   No frameworks detected");
        } else {
            for (FrameworkType framework : frameworks) {
                System.out.println("   ✓ " + framework.getDisplayName());
            }
        }
        System.out.println();
    }

    private void printProgress(String message) {
        if (verbose) {
            System.out.println("[INFO] " + message);
        }
    }

    private void printCoverage(CoverageReport report) {
        System.out.println("📈 Coverage update:");
        System.out.println("   Line: " + String.format("%.1f%%", report.overallLineCoverage() * 100));
        System.out.println("   Branch: " + String.format("%.1f%%", report.overallBranchCoverage() * 100));
        System.out.println();
    }

    private void printResult(OptimizationResult result) {
        System.out.println();
        System.out.println(result.getSummary());
        
        if (result.isSuccess()) {
            System.out.println("🎉 Target coverage achieved!");
        } else {
            System.out.println("⚠️ Target coverage not reached. Consider:");
            System.out.println("   - Increasing max iterations");
            System.out.println("   - Using AI-powered generation with --api-key");
            System.out.println("   - Manually reviewing uncovered code paths");
        }
    }

    private void printSummary(List<OptimizationResult> results) {
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

    private File getProjectRoot() {
        if (source.isFile()) {
            return source.getParentFile();
        }
        
        File current = source;
        while (current != null) {
            if (new File(current, "pom.xml").exists() || 
                new File(current, "build.gradle").exists()) {
                return current;
            }
            current = current.getParentFile();
        }
        
        return source;
    }
}
