package com.utagent.optimizer;

import com.utagent.build.BuildToolAdapter;
import com.utagent.build.BuildToolDetector;
import com.utagent.coverage.CoverageAnalyzer;
import com.utagent.exception.GenerationException;
import com.utagent.exception.ParseException;
import com.utagent.exception.UTAgentException;
import com.utagent.generator.TestGenerator;
import com.utagent.llm.TokenUsage;
import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.model.CoverageReport;
import com.utagent.model.ParsedTestFile;
import com.utagent.model.ParsedTestMethod;
import com.utagent.monitoring.GenerationPhase;
import com.utagent.monitoring.GenerationProgress;
import com.utagent.monitoring.LLMCallMonitor;
import com.utagent.parser.JavaCodeParser;
import com.utagent.parser.TestFileParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class IterativeOptimizer implements TestOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(IterativeOptimizer.class);

    private final JavaCodeParser codeParser;
    private final TestGenerator testGenerator;
    private final CoverageAnalyzer coverageAnalyzer;
    private final BuildToolAdapter buildToolAdapter;
    private final File projectRoot;
    private final File testOutputDir;

    private double targetCoverage;
    private int maxIterations;
    private final AtomicInteger currentIteration = new AtomicInteger(0);
    private boolean verbose;

    private Consumer<String> progressListener;
    private Consumer<CoverageReport> coverageListener;
    private Consumer<GenerationProgress> progressUpdateListener;
    private GenerationProgress generationProgress;
    private boolean incrementalMode = true;
    private final TestFileParser testFileParser;

    /**
     * 全依赖注入构造函数，便于测试和灵活配置
     */
    public IterativeOptimizer(File projectRoot,
                              JavaCodeParser codeParser,
                              TestGenerator testGenerator,
                              CoverageAnalyzer coverageAnalyzer,
                              BuildToolAdapter buildToolAdapter) {
        this.projectRoot = projectRoot;
        this.codeParser = codeParser;
        this.testGenerator = testGenerator;
        this.coverageAnalyzer = coverageAnalyzer;
        this.buildToolAdapter = buildToolAdapter != null ? buildToolAdapter : detectBuildTool(projectRoot);
        this.testOutputDir = new File(projectRoot, "target/generated-test-sources");
        this.targetCoverage = 0.80;
        this.maxIterations = 10;
        this.currentIteration.set(0);
        this.verbose = true;
        this.testFileParser = new TestFileParser();

        logger.info("Detected build tool: {}", this.buildToolAdapter.name());
    }

    /**
     * 便捷构造函数，自动创建依赖
     */
    public IterativeOptimizer(File projectRoot, String apiKey) {
        this(projectRoot,
             new JavaCodeParser(),
             new TestGenerator(apiKey),
             new CoverageAnalyzer(projectRoot),
             null);
    }
    
    private BuildToolAdapter detectBuildTool(File projectRoot) {
        Optional<BuildToolAdapter> detected = BuildToolDetector.detect(projectRoot);
        if (detected.isPresent()) {
            return detected.get();
        }
        
        logger.warn("Could not detect build tool, defaulting to Maven");
        return BuildToolDetector.getAdapter(com.utagent.build.BuildToolType.MAVEN);
    }

    @Override
    public TestOptimizer setTargetCoverage(double targetCoverage) {
        this.targetCoverage = Math.min(1.0, Math.max(0.0, targetCoverage));
        return this;
    }

    @Override
    public TestOptimizer setMaxIterations(int maxIterations) {
        this.maxIterations = Math.max(1, maxIterations);
        return this;
    }

    @Override
    public TestOptimizer setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    @Override
    public TestOptimizer setProgressListener(Consumer<String> progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    @Override
    public TestOptimizer setCoverageListener(Consumer<CoverageReport> coverageListener) {
        this.coverageListener = coverageListener;
        return this;
    }
    
    public TestOptimizer setProgressUpdateListener(Consumer<GenerationProgress> listener) {
        this.progressUpdateListener = listener;
        return this;
    }
    
    public TestOptimizer setIncrementalMode(boolean incrementalMode) {
        this.incrementalMode = incrementalMode;
        return this;
    }
    
    public boolean isIncrementalMode() {
        return incrementalMode;
    }
    
    public GenerationProgress getGenerationProgress() {
        return generationProgress;
    }

    public OptimizationResult optimize(File sourceFile) {
        return optimize(sourceFile, null);
    }

    public OptimizationResult optimize(File sourceFile, File existingTestFile) {
        if (incrementalMode && existingTestFile == null) {
            existingTestFile = findExistingTestFile(sourceFile);
        }
        
        if (incrementalMode && existingTestFile != null && existingTestFile.exists()) {
            return optimizeIncremental(sourceFile, existingTestFile);
        }
        
        return optimizeFull(sourceFile);
    }

    private File findExistingTestFile(File sourceFile) {
        File testSourceDir = buildToolAdapter.getTestSourceDirectory(projectRoot);
        if (testSourceDir == null) {
            testSourceDir = new File(projectRoot, "src/test/java");
        }
        
        Optional<File> existingTest = testFileParser.findExistingTestFile(sourceFile, testSourceDir);
        return existingTest.orElse(null);
    }

    public OptimizationResult optimizeIncremental(File sourceFile, File existingTestFile) {
        logger.info("Starting incremental optimization for: {}", sourceFile.getAbsolutePath());
        
        generationProgress = new GenerationProgress(
            sourceFile.getAbsolutePath(),
            sourceFile.getName()
        );
        generationProgress.setMaxIterations(maxIterations);
        generationProgress.setTargetCoverage(targetCoverage);
        
        generationProgress.setPhase(GenerationPhase.PARSING, "Parsing source and existing test files");
        notifyProgressUpdate();
        
        notifyProgress("Starting incremental optimization for: " + sourceFile.getName());
        
        OptimizationResult result = new OptimizationResult();
        result.setSourceFile(sourceFile);
        result.setExistingTestFile(existingTestFile);
        
        var parsedClass = codeParser.parseFile(sourceFile);
        if (parsedClass.isEmpty()) {
            logger.error("Failed to parse source file: {}", sourceFile.getAbsolutePath());
            result.setSuccess(false);
            result.setErrorMessage("Failed to parse source file");
            generationProgress.setError(new GenerationException("Failed to parse source file"));
            notifyProgressUpdate();
            return result;
        }
        
        ClassInfo classInfo = parsedClass.get();
        result.setClassInfo(classInfo);
        
        Optional<ParsedTestFile> parsedTestOpt = testFileParser.parse(existingTestFile);
        if (parsedTestOpt.isEmpty()) {
            logger.warn("Failed to parse existing test file, falling back to full generation");
            return optimizeFull(sourceFile);
        }
        
        ParsedTestFile existingTests = parsedTestOpt.get();
        result.setParsedTestFile(existingTests);
        
        logger.info("Found {} existing test methods in {}", 
                   existingTests.getTestMethodCount(), existingTestFile.getName());
        
        generationProgress.setPhase(GenerationPhase.TEST_GENERATION, "Generating incremental tests");
        notifyProgressUpdate();
        
        String additionalTests = testGenerator.generateIncrementalTests(classInfo, existingTests, List.of());
        
        TokenUsage tokenUsage = testGenerator.getTotalTokenUsage();
        generationProgress.addTokenUsage(tokenUsage);
        generationProgress.incrementLlmCalls();
        
        File testFile;
        if (additionalTests != null && !additionalTests.trim().isEmpty()) {
            generationProgress.setPhase(GenerationPhase.WRITING_TEST, "Merging test file");
            notifyProgressUpdate();
            
            testFile = mergeTestFile(existingTestFile, additionalTests);
            result.setAddedTestMethods(extractMethodNames(additionalTests));
            notifyProgress("Merged additional tests into existing test file");
        } else {
            testFile = existingTestFile;
            notifyProgress("No additional tests needed - existing tests are sufficient");
        }
        
        result.setGeneratedTestFile(testFile);
        
        generationProgress.setPhase(GenerationPhase.RUNNING_TESTS, "Running tests");
        notifyProgressUpdate();
        
        CoverageReport currentCoverage = runTestsAndGetCoverage();
        result.addCoverageReport(currentIteration.get(), currentCoverage);
        
        generationProgress.setCoverage(currentCoverage);
        generationProgress.setPhase(GenerationPhase.COVERAGE_ANALYSIS, "Analyzing coverage");
        notifyProgressUpdate();
        
        notifyCoverage(currentCoverage);
        
        while (!meetsTarget(currentCoverage) && currentIteration.get() < maxIterations) {
            int iteration = currentIteration.incrementAndGet();
            generationProgress.setIteration(iteration);
            generationProgress.setPhase(GenerationPhase.OPTIMIZATION, 
                "Optimization iteration " + iteration,
                String.format("Current coverage: %.1f%%", currentCoverage.overallLineCoverage() * 100));
            notifyProgressUpdate();
            
            notifyProgress("Iteration " + iteration + ": Current coverage " + 
                String.format("%.1f%%", currentCoverage.overallLineCoverage() * 100));
            
            List<CoverageInfo> uncoveredInfo = getUncoveredInfo(currentCoverage, classInfo);
            
            if (uncoveredInfo.isEmpty()) {
                notifyProgress("No more uncovered code to improve");
                break;
            }
            
            generationProgress.setPhase(GenerationPhase.LLM_CALL, "Generating additional tests");
            notifyProgressUpdate();
            
            Set<String> existingMethodNames = getExistingTestMethodNames(testFile);
            String moreTests = testGenerator.generateAdditionalTestsAvoidingDuplicates(
                classInfo, uncoveredInfo, existingMethodNames);
            
            tokenUsage = testGenerator.getTotalTokenUsage();
            generationProgress.addTokenUsage(tokenUsage);
            generationProgress.incrementLlmCalls();
            
            if (moreTests != null && !moreTests.isEmpty()) {
                appendTestsToFile(testFile, moreTests);
                notifyProgress("Added additional tests for uncovered code");
            }
            
            generationProgress.setPhase(GenerationPhase.RUNNING_TESTS, "Running tests");
            notifyProgressUpdate();
            
            currentCoverage = runTestsAndGetCoverage();
            result.addCoverageReport(currentIteration.get(), currentCoverage);
            
            generationProgress.setCoverage(currentCoverage);
            generationProgress.setPhase(GenerationPhase.COVERAGE_ANALYSIS, "Analyzing coverage");
            notifyProgressUpdate();
            
            notifyCoverage(currentCoverage);
        }
        
        result.setFinalCoverage(currentCoverage);
        result.setSuccess(meetsTarget(currentCoverage));
        result.setIterations(currentIteration.get());
        
        if (result.isSuccess()) {
            generationProgress.setPhase(GenerationPhase.COMPLETED, "Target coverage achieved");
            notifyProgress("Target coverage achieved: " + 
                String.format("%.1f%%", currentCoverage.overallLineCoverage() * 100));
        } else {
            generationProgress.setPhase(GenerationPhase.COMPLETED, "Max iterations reached");
            notifyProgress("Max iterations reached. Final coverage: " + 
                String.format("%.1f%%", currentCoverage.overallLineCoverage() * 100));
        }
        
        notifyProgressUpdate();
        
        return result;
    }

    public OptimizationResult optimizeFull(File sourceFile) {
        logger.info("Starting full optimization for: {}", sourceFile.getAbsolutePath());
        
        generationProgress = new GenerationProgress(
            sourceFile.getAbsolutePath(),
            sourceFile.getName()
        );
        generationProgress.setMaxIterations(maxIterations);
        generationProgress.setTargetCoverage(targetCoverage);
        
        generationProgress.setPhase(GenerationPhase.PARSING, "Parsing source file");
        notifyProgressUpdate();
        
        notifyProgress("Starting optimization for: " + sourceFile.getName());
        
        OptimizationResult result = new OptimizationResult();
        result.setSourceFile(sourceFile);
        
        var parsedClass = codeParser.parseFile(sourceFile);
        if (parsedClass.isEmpty()) {
            logger.error("Failed to parse source file: {}", sourceFile.getAbsolutePath());
            result.setSuccess(false);
            result.setErrorMessage("Failed to parse source file");
            generationProgress.setError(new GenerationException("Failed to parse source file"));
            notifyProgressUpdate();
            return result;
        }
        
        ClassInfo classInfo = parsedClass.get();
        result.setClassInfo(classInfo);
        generationProgress.setPhase(GenerationPhase.TEST_GENERATION, "Generating initial tests");
        notifyProgressUpdate();
        
        String testCode = testGenerator.generateTestClass(classInfo);
        
        TokenUsage tokenUsage = testGenerator.getTotalTokenUsage();
        generationProgress.addTokenUsage(tokenUsage);
        generationProgress.incrementLlmCalls();
        
        generationProgress.setPhase(GenerationPhase.WRITING_TEST, "Writing test file");
        notifyProgressUpdate();
        
        File testFile = writeTestFile(classInfo, testCode);
        result.setGeneratedTestFile(testFile);
        
        int methodCount = countTestMethods(testCode);
        generationProgress.incrementTestMethods(methodCount);
        generationProgress.incrementTestClasses();
        
        notifyProgress("Generated initial test file: " + testFile.getName());
        
        generationProgress.setPhase(GenerationPhase.RUNNING_TESTS, "Running tests");
        notifyProgressUpdate();
        
        CoverageReport currentCoverage = runTestsAndGetCoverage();
        result.addCoverageReport(currentIteration.get(), currentCoverage);
        
        generationProgress.setCoverage(currentCoverage);
        generationProgress.setPhase(GenerationPhase.COVERAGE_ANALYSIS, "Analyzing coverage");
        notifyProgressUpdate();
        
        notifyCoverage(currentCoverage);
        
        while (!meetsTarget(currentCoverage) && currentIteration.get() < maxIterations) {
            int iteration = currentIteration.incrementAndGet();
            generationProgress.setIteration(iteration);
            generationProgress.setPhase(GenerationPhase.OPTIMIZATION, 
                "Optimization iteration " + iteration,
                String.format("Current coverage: %.1f%%", currentCoverage.overallLineCoverage() * 100));
            notifyProgressUpdate();
            
            notifyProgress("Iteration " + iteration + ": Current coverage " + 
                String.format("%.1f%%", currentCoverage.overallLineCoverage() * 100));
            
            List<CoverageInfo> uncoveredInfo = getUncoveredInfo(currentCoverage, classInfo);
            
            if (uncoveredInfo.isEmpty()) {
                notifyProgress("No more uncovered code to improve");
                break;
            }
            
            generationProgress.setPhase(GenerationPhase.LLM_CALL, "Generating additional tests");
            notifyProgressUpdate();
            
            String additionalTests = testGenerator.generateAdditionalTests(classInfo, uncoveredInfo);
            
            tokenUsage = testGenerator.getTotalTokenUsage();
            generationProgress.addTokenUsage(tokenUsage);
            generationProgress.incrementLlmCalls();
            
            if (additionalTests != null && !additionalTests.isEmpty()) {
                appendTestsToFile(testFile, additionalTests);
                int additionalMethodCount = countTestMethods(additionalTests);
                generationProgress.incrementTestMethods(additionalMethodCount);
                notifyProgress("Added additional tests for uncovered code");
            }
            
            generationProgress.setPhase(GenerationPhase.RUNNING_TESTS, "Running tests");
            notifyProgressUpdate();
            
            currentCoverage = runTestsAndGetCoverage();
            result.addCoverageReport(currentIteration.get(), currentCoverage);
            
            generationProgress.setCoverage(currentCoverage);
            generationProgress.setPhase(GenerationPhase.COVERAGE_ANALYSIS, "Analyzing coverage");
            notifyProgressUpdate();
            
            notifyCoverage(currentCoverage);
        }
        
        result.setFinalCoverage(currentCoverage);
        result.setSuccess(meetsTarget(currentCoverage));
        result.setIterations(currentIteration.get());
        
        if (result.isSuccess()) {
            generationProgress.setPhase(GenerationPhase.COMPLETED, "Target coverage achieved");
            notifyProgress("Target coverage achieved: " + 
                String.format("%.1f%%", currentCoverage.overallLineCoverage() * 100));
        } else {
            generationProgress.setPhase(GenerationPhase.COMPLETED, "Max iterations reached");
            notifyProgress("Max iterations reached. Final coverage: " + 
                String.format("%.1f%%", currentCoverage.overallLineCoverage() * 100));
        }
        
        notifyProgressUpdate();
        
        return result;
    }

    public List<OptimizationResult> optimizeDirectory(File sourceDirectory) {
        List<OptimizationResult> results = new ArrayList<>();
        
        List<File> javaFiles = findJavaFiles(sourceDirectory);
        notifyProgress("Found " + javaFiles.size() + " Java files to process");
        
        generationProgress = new GenerationProgress(
            sourceDirectory.getAbsolutePath(),
            sourceDirectory.getName()
        );
        generationProgress.setTotalFiles(javaFiles.size());
        generationProgress.setMaxIterations(maxIterations);
        generationProgress.setTargetCoverage(targetCoverage);
        
        int fileIndex = 0;
        for (File javaFile : javaFiles) {
            fileIndex++;
            try {
                generationProgress.setPhase(GenerationPhase.PARSING, 
                    "Processing file " + fileIndex + "/" + javaFiles.size(),
                    javaFile.getName());
                generationProgress.setFilesProcessed(fileIndex);
                notifyProgressUpdate();
                
                OptimizationResult result = optimize(javaFile);
                results.add(result);
            } catch (Exception e) {
                logger.error("Error optimizing file: {}", javaFile.getAbsolutePath(), e);
            }
        }
        
        return results;
    }

    private File writeTestFile(ClassInfo classInfo, String testCode) {
        try {
            Path testPath = determineTestPath(classInfo);
            if (testPath == null) {
                throw new GenerationException("Failed to determine test path for class: " + classInfo.className());
            }
            Files.createDirectories(testPath.getParent());

            File testFile = testPath.toFile();
            Files.writeString(testFile.toPath(), testCode, java.nio.charset.StandardCharsets.UTF_8);

            logger.info("Generated test file: {}", testFile.getAbsolutePath());
            return testFile;
        } catch (IOException e) {
            throw new GenerationException("Failed to write test file for class: " + classInfo.className(), e);
        }
    }

    private Path determineTestPath(ClassInfo classInfo) {
        String packagePath = classInfo.packageName().replace('.', File.separatorChar);
        String testClassName = classInfo.className() + "Test.java";
        
        File testSourceDir = buildToolAdapter.getTestSourceDirectory(projectRoot);
        if (testSourceDir == null) {
            testSourceDir = new File(projectRoot, "src/test/java");
        }
        
        return testSourceDir.toPath()
            .resolve(packagePath)
            .resolve(testClassName);
    }

    private void appendTestsToFile(File testFile, String additionalTests) {
        try {
            String existingContent = Files.readString(testFile.toPath());
            
            int lastBraceIndex = existingContent.lastIndexOf('}');
            if (lastBraceIndex > 0) {
                String newContent = existingContent.substring(0, lastBraceIndex) + 
                    "\n" + additionalTests + "\n}\n";
                Files.writeString(testFile.toPath(), newContent);
            }
        } catch (IOException e) {
            logger.error("Error appending tests to file", e);
        }
    }

    private CoverageReport runTestsAndGetCoverage() {
        try {
            String command = buildToolAdapter.getCoverageCommand();
            String[] commandParts = parseCommand(command);
            
            ProcessBuilder pb = new ProcessBuilder(commandParts);
            pb.directory(projectRoot);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            try {
                int exitCode = process.waitFor();
                
                if (exitCode != 0) {
                    logger.warn("Tests failed with exit code: {}", exitCode);
                }
            } finally {
                closeProcessStreams(process);
            }
            
            File coverageReport = buildToolAdapter.getCoverageReportFile(projectRoot);
            if (coverageReport != null && coverageReport.exists()) {
                return coverageAnalyzer.analyzeFromJacocoXml(coverageReport);
            }
            
            File execFile = buildToolAdapter.getCoverageExecFile(projectRoot);
            if (execFile != null && execFile.exists()) {
                return coverageAnalyzer.analyzeCoverage(execFile);
            }
            
            return new CoverageReport();
        } catch (IOException e) {
            logger.error("IO error running tests", e);
            return new CoverageReport();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Test execution interrupted", e);
            return new CoverageReport();
        }
    }
    
    private void closeProcessStreams(Process process) {
        try {
            if (process.getInputStream() != null) {
                process.getInputStream().close();
            }
        } catch (IOException e) {
            logger.debug("Failed to close process input stream", e);
        }
        try {
            if (process.getOutputStream() != null) {
                process.getOutputStream().close();
            }
        } catch (IOException e) {
            logger.debug("Failed to close process output stream", e);
        }
        try {
            if (process.getErrorStream() != null) {
                process.getErrorStream().close();
            }
        } catch (IOException e) {
            logger.debug("Failed to close process error stream", e);
        }
    }
    
    private String[] parseCommand(String command) {
        if (command.startsWith("./")) {
            String[] parts = command.split("\\s+");
            return parts;
        }
        return command.split("\\s+");
    }

    private List<CoverageInfo> getUncoveredInfo(CoverageReport report, ClassInfo classInfo) {
        List<CoverageInfo> uncoveredInfo = new ArrayList<>();
        
        for (CoverageInfo info : report.classCoverages()) {
            if (info.className().equals(classInfo.fullyQualifiedName()) ||
                info.className().equals(classInfo.className())) {
                if (info.getLineCoverageRate() < 1.0) {
                    uncoveredInfo.add(info);
                }
            }
        }
        
        return uncoveredInfo;
    }

    private boolean meetsTarget(CoverageReport report) {
        return report.overallLineCoverage() >= targetCoverage;
    }

    private List<File> findJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        
        if (directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> 
                name.endsWith(".java") && !name.endsWith("Test.java"));
            
            if (files != null) {
                for (File file : files) {
                    javaFiles.add(file);
                }
            }
            
            File[] subDirs = directory.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    javaFiles.addAll(findJavaFiles(subDir));
                }
            }
        }
        
        return javaFiles;
    }

    private void notifyProgress(String message) {
        if (verbose) {
            logger.info(message);
        }
        if (progressListener != null) {
            progressListener.accept(message);
        }
    }

    private void notifyCoverage(CoverageReport report) {
        if (coverageListener != null) {
            coverageListener.accept(report);
        }
    }
    
    private void notifyProgressUpdate() {
        if (progressUpdateListener != null && generationProgress != null) {
            progressUpdateListener.accept(generationProgress);
        }
    }

    @Override
    public double getTargetCoverage() {
        return targetCoverage;
    }

    @Override
    public int getCurrentIteration() {
        return currentIteration.get();
    }

    @Override
    public int getMaxIterations() {
        return maxIterations;
    }

    @Override
    public String getBuildToolName() {
        return buildToolAdapter.name();
    }

    private File mergeTestFile(File existingTestFile, String additionalTests) {
        try {
            String existingContent = Files.readString(existingTestFile.toPath());
            
            String additionalMethods = extractMethodBodies(additionalTests);
            
            int lastBraceIndex = existingContent.lastIndexOf('}');
            if (lastBraceIndex > 0) {
                String newContent = existingContent.substring(0, lastBraceIndex) + 
                    "\n" + additionalMethods + "\n}\n";
                Files.writeString(existingTestFile.toPath(), newContent);
            }
            
            return existingTestFile;
        } catch (IOException e) {
            logger.error("Error merging test file", e);
            return existingTestFile;
        }
    }

    private String extractMethodBodies(String testCode) {
        if (testCode == null || testCode.trim().isEmpty()) {
            return "";
        }

        if (testCode.contains("public class") || testCode.contains("class ")) {
            java.util.regex.Pattern classPattern = java.util.regex.Pattern.compile("class\\s+\\w+\\s*\\{");
            java.util.regex.Matcher classMatcher = classPattern.matcher(testCode);
            
            if (classMatcher.find()) {
                int classStart = classMatcher.end();
                int depth = 1;
                int i = classStart;
                while (i < testCode.length() && depth > 0) {
                    char c = testCode.charAt(i);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    i++;
                }
                return testCode.substring(classStart, i - 1).trim();
            }
        }

        return testCode.trim();
    }

    private Set<String> getExistingTestMethodNames(File testFile) {
        Set<String> methodNames = new HashSet<>();
        
        try {
            String content = Files.readString(testFile.toPath());
            java.util.regex.Pattern methodPattern = java.util.regex.Pattern.compile(
                "(?:@Test|@ParameterizedTest)[^}]*?void\\s+(\\w+)\\s*\\(",
                java.util.regex.Pattern.DOTALL
            );
            
            java.util.regex.Matcher matcher = methodPattern.matcher(content);
            while (matcher.find()) {
                methodNames.add(matcher.group(1));
            }
        } catch (IOException e) {
            logger.debug("Failed to read test file for method names: {}", e.getMessage());
        }
        
        return methodNames;
    }

    private List<String> extractMethodNames(String testCode) {
        List<String> methodNames = new ArrayList<>();
        java.util.regex.Pattern methodPattern = java.util.regex.Pattern.compile(
            "(?:@Test|@ParameterizedTest)[^}]*?void\\s+(\\w+)\\s*\\(",
            java.util.regex.Pattern.DOTALL
        );
        
        java.util.regex.Matcher matcher = methodPattern.matcher(testCode);
        while (matcher.find()) {
            methodNames.add(matcher.group(1));
        }
        
        return methodNames;
    }
    
    private int countTestMethods(String testCode) {
        if (testCode == null || testCode.isEmpty()) {
            return 0;
        }
        return extractMethodNames(testCode).size();
    }
}
