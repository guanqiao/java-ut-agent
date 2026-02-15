package com.utagent.cli;

import com.utagent.config.AgentConfig;
import com.utagent.config.ConfigLoader;
import com.utagent.config.CoverageConfig;
import com.utagent.config.GenerationConfig;
import com.utagent.config.OutputConfig;
import com.utagent.coverage.CoverageAnalyzer;
import com.utagent.generator.TestGenerator;
import com.utagent.llm.LLMConfig;
import com.utagent.llm.LLMProviderFactory;
import com.utagent.llm.LLMProviderType;
import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageReport;
import com.utagent.optimizer.IterativeOptimizer;
import com.utagent.optimizer.OptimizationResult;
import com.utagent.optimizer.TestOptimizer;
import com.utagent.parser.FrameworkDetector;
import com.utagent.parser.FrameworkType;
import com.utagent.parser.JavaCodeParser;
import com.utagent.util.SensitiveDataMasker;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
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
    private Double targetCoverage;

    @Option(names = {"-i", "--iterations"}, description = "Maximum iterations for optimization (default: 10)")
    private Integer maxIterations;

    @Option(names = {"-a", "--api-key"}, description = "API key for LLM provider")
    private String apiKey;

    @Option(names = {"--api-url"}, description = "Custom API URL")
    private String apiUrl;

    @Option(names = {"-p", "--provider"}, description = "LLM provider: openai, claude, ollama, deepseek (default: openai)")
    private String provider;

    @Option(names = {"--model"}, description = "LLM model to use")
    private String model;

    @Option(names = {"-o", "--output"}, description = "Output directory for generated tests")
    private File outputDir;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private Boolean verbose;

    @Option(names = {"--dry-run"}, description = "Generate tests without running them")
    private boolean dryRun = false;

    @Option(names = {"--analyze-only"}, description = "Only analyze coverage, don't generate tests")
    private boolean analyzeOnly = false;

    @Option(names = {"--detect-framework"}, description = "Detect frameworks used in the source code")
    private boolean detectFramework = false;

    @Option(names = {"--init"}, description = "Initialize configuration file")
    private boolean initConfig = false;

    @Option(names = {"--stream"}, description = "Enable streaming output")
    private boolean stream = false;

    @Option(names = {"--config"}, description = "Path to configuration file")
    private File configFile;

    private AgentConfig config;

    @Override
    public Integer call() throws Exception {
        loadConfiguration();
        
        if (initConfig) {
            return initializeConfig();
        }
        
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

    private void loadConfiguration() {
        File projectRoot = source != null ? getProjectRoot() : new File(".");
        
        if (configFile != null && configFile.exists()) {
            config = ConfigLoader.loadFromFile(configFile);
        } else {
            config = ConfigLoader.load(projectRoot);
        }
        
        config = applyCliOverrides(config);
    }

    private AgentConfig applyCliOverrides(AgentConfig baseConfig) {
        AgentConfig.Builder builder = AgentConfig.builder();
        
        LLMConfig.Builder llmBuilder = LLMConfig.builder()
            .provider(provider != null ? provider : baseConfig.getLlm().provider())
            .apiKey(apiKey != null ? apiKey : baseConfig.getLlm().apiKey())
            .baseUrl(apiUrl != null ? apiUrl : baseConfig.getLlm().baseUrl())
            .model(model != null ? model : baseConfig.getLlm().model())
            .temperature(baseConfig.getLlm().temperature())
            .maxTokens(baseConfig.getLlm().maxTokens())
            .maxRetries(baseConfig.getLlm().maxRetries());
        builder.llm(llmBuilder.build());
        
        CoverageConfig.Builder coverageBuilder = CoverageConfig.builder()
            .target(targetCoverage != null ? targetCoverage : baseConfig.getCoverage().target())
            .maxIterations(maxIterations != null ? maxIterations : baseConfig.getCoverage().maxIterations())
            .includeBranchCoverage(baseConfig.getCoverage().includeBranchCoverage());
        builder.coverage(coverageBuilder.build());
        
        OutputConfig.Builder outputBuilder = OutputConfig.builder()
            .directory(outputDir != null ? outputDir.getPath() : baseConfig.getOutput().directory())
            .format(baseConfig.getOutput().format())
            .verbose(verbose != null ? verbose : baseConfig.getOutput().verbose())
            .colorOutput(baseConfig.getOutput().colorOutput())
            .showProgress(baseConfig.getOutput().getShowProgressOrDefault());
        builder.output(outputBuilder.build());
        
        builder.generation(baseConfig.getGeneration());
        
        return builder.build();
    }

    private int initializeConfig() {
        File projectRoot = source != null ? source : new File(".");

        try {
            ConfigLoader.createDefaultConfig(projectRoot);
            System.out.println("✅ Created default configuration file: " +
                new File(projectRoot, ".java-ut-agent.yaml").getAbsolutePath());
            System.out.println("⚠️  Please edit the configuration file and add your API key.");
            return 0;
        } catch (IOException e) {
            System.err.println("❌ Failed to create configuration file: " + e.getMessage());
            return 1;
        }
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

    private int generateTests() {
        System.out.println("🚀 Java UT Agent - Starting test generation...");
        System.out.println();
        
        LLMConfig llmConfig = config.getLlm();
        LLMProviderType providerType = LLMProviderType.fromId(llmConfig.provider());
        
        if (llmConfig.apiKey() != null || providerType == LLMProviderType.OLLAMA) {
            System.out.println("✨ Using " + providerType.getDisplayName() + " provider");
            if (llmConfig.model() != null) {
                System.out.println("📝 Model: " + llmConfig.getModelOrDefault());
            }
        } else {
            System.out.println("📝 Using template-based generation (set --api-key for AI-powered generation)");
        }
        
        System.out.println("🎯 Target coverage: " + String.format("%.0f%%", config.getCoverage().getTargetOrDefault() * 100));
        System.out.println("🔄 Max iterations: " + config.getCoverage().getMaxIterationsOrDefault());
        System.out.println();

        TestOptimizer optimizer = new IterativeOptimizer(getProjectRoot(), resolveApiKey())
            .setTargetCoverage(config.getCoverage().getTargetOrDefault())
            .setMaxIterations(config.getCoverage().getMaxIterationsOrDefault())
            .setVerbose(config.getOutput().getVerboseOrDefault())
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

    private String resolveApiKey() {
        LLMConfig llmConfig = config.getLlm();
        if (llmConfig.apiKey() != null) {
            return llmConfig.apiKey();
        }
        
        LLMProviderType providerType = LLMProviderType.fromId(llmConfig.provider());
        return switch (providerType) {
            case OPENAI -> System.getenv("OPENAI_API_KEY");
            case CLAUDE -> System.getenv("ANTHROPIC_API_KEY");
            case DEEPSEEK -> System.getenv("DEEPSEEK_API_KEY");
            case OLLAMA -> null;
        };
    }

    private int generateTestsDryRun() {
        System.out.println("📋 Dry run mode - generating tests without execution");
        System.out.println();

        JavaCodeParser parser = new JavaCodeParser();
        TestGenerator generator = createTestGenerator();

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

    private TestGenerator createTestGenerator() {
        String apiKey = resolveApiKey();
        LLMConfig llmConfig = config.getLlm();
        
        return new TestGenerator(
            apiKey,
            llmConfig.provider(),
            llmConfig.baseUrl(),
            llmConfig.getModelOrDefault()
        );
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
        
        double target = config.getCoverage().getTargetOrDefault();
        if (report.meetsTarget(target)) {
            System.out.println("✅ Coverage meets target: " + String.format("%.0f%%", target * 100));
        } else {
            System.out.println("❌ Coverage below target: " + String.format("%.0f%%", target * 100));
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
        if (config.getOutput().getVerboseOrDefault()) {
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
            System.out.println("   - Trying a different LLM provider with --provider");
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
        if (source == null) {
            return new File(".");
        }
        
        if (source.isFile()) {
            return source.getParentFile();
        }
        
        File current = source;
        while (current != null) {
            if (new File(current, "pom.xml").exists() || 
                new File(current, "build.gradle").exists() ||
                new File(current, "build.gradle.kts").exists()) {
                return current;
            }
            current = current.getParentFile();
        }
        
        return source;
    }
}
