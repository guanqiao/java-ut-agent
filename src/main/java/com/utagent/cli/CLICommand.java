package com.utagent.cli;

import com.utagent.config.AgentConfig;
import com.utagent.coverage.CoverageAnalyzer;
import com.utagent.generator.TestGenerator;
import com.utagent.llm.LLMConfig;
import com.utagent.llm.LLMProviderType;
import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageReport;
import com.utagent.monitoring.GenerationProgress;
import com.utagent.monitoring.LLMCallMonitor;
import com.utagent.monitoring.RealTimeDashboard;
import com.utagent.optimizer.IterativeOptimizer;
import com.utagent.optimizer.OptimizationResult;
import com.utagent.optimizer.TestOptimizer;
import com.utagent.parser.FrameworkDetector;
import com.utagent.parser.FrameworkType;
import com.utagent.parser.JavaCodeParser;
import com.utagent.util.ApiKeyResolver;
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
    
    @Option(names = {"--dashboard"}, description = "Enable real-time dashboard display")
    private boolean enableDashboard = false;

    @Option(names = {"--incremental", "-I"}, description = "Enable incremental test generation (preserve existing tests, default: true)")
    private Boolean incremental;

    @Option(names = {"--force", "-F"}, description = "Force full regeneration of tests (ignore existing tests)")
    private boolean force = false;

    private AgentConfig config;
    private ConfigManager configManager;
    private OutputFormatter outputFormatter;
    private RealTimeDashboard dashboard;

    @Override
    public Integer call() throws Exception {
        initializeManagers();
        loadConfiguration();
        
        if (initConfig) {
            return initializeConfig();
        }
        
        if (source == null) {
            outputFormatter.printBanner();
            return 0;
        }

        if (!source.exists()) {
            outputFormatter.printSourceNotFoundError(source.getAbsolutePath());
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

    private void initializeManagers() {
        configManager = new ConfigManager(
            source,
            configFile,
            targetCoverage,
            maxIterations,
            apiKey,
            apiUrl,
            provider,
            model,
            outputDir,
            verbose
        );
    }

    private void loadConfiguration() {
        config = configManager.loadConfiguration();
        outputFormatter = new OutputFormatter(config.getOutput());
    }

    private int initializeConfig() {
        int result = configManager.initializeConfig();
        if (result == 0) {
            outputFormatter.printConfigInitSuccess(configManager.getConfigFilePath());
        } else {
            outputFormatter.printConfigInitFailure("Failed to create configuration file");
        }
        return result;
    }

    private int generateTests() {
        outputFormatter.printGenerationStart();
        
        LLMConfig llmConfig = config.getLlm();
        LLMProviderType providerType = LLMProviderType.fromId(llmConfig.provider());
        
        String providerName = providerType.getDisplayName();
        String modelName = llmConfig.model() != null ? llmConfig.getModelOrDefault() : null;
        outputFormatter.printProviderInfo(providerName, modelName);
        
        outputFormatter.printTargetInfo(
            config.getCoverage().getTargetOrDefault(),
            config.getCoverage().getMaxIterationsOrDefault()
        );

        boolean useIncremental = determineIncrementalMode();
        outputFormatter.printIncrementalMode(useIncremental);

        IterativeOptimizer optimizer = (IterativeOptimizer) new IterativeOptimizer(configManager.getProjectRoot(), resolveApiKey())
            .setTargetCoverage(config.getCoverage().getTargetOrDefault())
            .setMaxIterations(config.getCoverage().getMaxIterationsOrDefault())
            .setVerbose(config.getOutput().getVerboseOrDefault())
            .setIncrementalMode(useIncremental)
            .setProgressListener(outputFormatter::printProgress)
            .setCoverageListener(outputFormatter::printCoverage);

        if (enableDashboard) {
            dashboard = RealTimeDashboard.builder().build();
            optimizer.setProgressUpdateListener(progress -> {
                dashboard.update();
            });
        }

        if (dryRun) {
            return generateTestsDryRun();
        }

        OptimizationResult result;
        try {
            if (enableDashboard) {
                GenerationProgress initialProgress = new GenerationProgress(
                    source.getAbsolutePath(),
                    source.getName()
                );
                initialProgress.setMaxIterations(config.getCoverage().getMaxIterationsOrDefault());
                initialProgress.setTargetCoverage(config.getCoverage().getTargetOrDefault());
                dashboard.start(initialProgress);
            }
            
            if (source.isFile()) {
                result = optimizer.optimize(source);
                outputFormatter.printResult(result);
            } else {
                List<OptimizationResult> results = optimizer.optimizeDirectory(source);
                outputFormatter.printSummary(results);
            }
        } finally {
            if (enableDashboard && dashboard != null) {
                dashboard.stop();
            }
        }

        printLLMSummary();

        return 0;
    }
    
    private void printLLMSummary() {
        LLMCallMonitor monitor = LLMCallMonitor.getInstance();
        LLMCallMonitor.Statistics stats = monitor.getStatistics();
        
        if (stats.totalCalls() > 0) {
            System.out.println();
            System.out.println("=== LLM Usage Summary ===");
            System.out.println("Total calls: " + stats.totalCalls());
            System.out.println("Successful: " + stats.successfulCalls());
            System.out.println("Failed: " + stats.failedCalls());
            System.out.println("Total tokens: " + stats.getFormattedTokens());
            System.out.println("Average latency: " + String.format("%.0fms", stats.averageLatencyMs()));
            System.out.println("Estimated cost: $" + String.format("%.4f", stats.estimatedCost()));
        }
    }

    private String resolveApiKey() {
        LLMConfig llmConfig = config.getLlm();
        LLMProviderType providerType = LLMProviderType.fromId(llmConfig.provider());
        return ApiKeyResolver.resolve(llmConfig.apiKey(), providerType);
    }

    private boolean determineIncrementalMode() {
        if (force) {
            return false;
        }
        if (incremental != null) {
            return incremental;
        }
        return config.getGeneration().getIncrementalOrDefault();
    }

    private int generateTestsDryRun() {
        outputFormatter.printDryRunMessage();

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
        outputFormatter.printCoverageAnalysisStart();

        File projectRoot = configManager.getProjectRoot();
        CoverageAnalyzer analyzer = new CoverageAnalyzer(projectRoot);

        File jacocoXml = new File(projectRoot, "target/site/jacoco/jacoco.xml");
        File execFile = new File(projectRoot, "target/jacoco.exec");

        CoverageReport report;
        if (jacocoXml.exists()) {
            report = analyzer.analyzeFromJacocoXml(jacocoXml);
        } else if (execFile.exists()) {
            report = analyzer.analyzeCoverage(execFile);
        } else {
            outputFormatter.printCoverageDataNotFoundError();
            return 1;
        }

        System.out.println(analyzer.formatCoverageReport(report));
        
        double target = config.getCoverage().getTargetOrDefault();
        outputFormatter.printCoverageTargetStatus(target, report.meetsTarget(target));

        return 0;
    }

    private int detectFrameworks() {
        outputFormatter.printFrameworkDetectionStart();

        JavaCodeParser parser = new JavaCodeParser();
        FrameworkDetector detector = new FrameworkDetector();

        if (source.isFile()) {
            var classInfo = parser.parseFile(source);
            if (classInfo.isPresent()) {
                Set<FrameworkType> frameworks = detector.detectFrameworks(classInfo.get());
                outputFormatter.printFrameworks(classInfo.get().className(), frameworks);
            }
        } else {
            List<ClassInfo> classes = parser.parseDirectory(source);
            for (ClassInfo classInfo : classes) {
                Set<FrameworkType> frameworks = detector.detectFrameworks(classInfo);
                outputFormatter.printFrameworks(classInfo.fullyQualifiedName(), frameworks);
            }
        }

        return 0;
    }
}

