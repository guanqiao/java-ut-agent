package com.utagent.optimizer;

import com.utagent.build.BuildToolAdapter;
import com.utagent.build.BuildToolDetector;
import com.utagent.coverage.CoverageAnalyzer;
import com.utagent.exception.GenerationException;
import com.utagent.exception.ParseException;
import com.utagent.exception.UTAgentException;
import com.utagent.generator.TestGenerator;
import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.model.CoverageReport;
import com.utagent.parser.JavaCodeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    public OptimizationResult optimize(File sourceFile) {
        return optimize(sourceFile, null);
    }

    public OptimizationResult optimize(File sourceFile, File existingTestFile) {
        logger.info("Starting optimization for: {}", sourceFile.getAbsolutePath());
        notifyProgress("Starting optimization for: " + sourceFile.getName());
        
        OptimizationResult result = new OptimizationResult();
        result.setSourceFile(sourceFile);
        
        var parsedClass = codeParser.parseFile(sourceFile);
        if (parsedClass.isEmpty()) {
            logger.error("Failed to parse source file: {}", sourceFile.getAbsolutePath());
            result.setSuccess(false);
            result.setErrorMessage("Failed to parse source file");
            return result;
        }
        
        ClassInfo classInfo = parsedClass.get();
        result.setClassInfo(classInfo);
        
        String testCode = testGenerator.generateTestClass(classInfo);
        File testFile = writeTestFile(classInfo, testCode);
        result.setGeneratedTestFile(testFile);
        
        notifyProgress("Generated initial test file: " + testFile.getName());
        
        CoverageReport currentCoverage = runTestsAndGetCoverage();
        result.addCoverageReport(currentIteration.get(), currentCoverage);
        
        notifyCoverage(currentCoverage);
        
        while (!meetsTarget(currentCoverage) && currentIteration.get() < maxIterations) {
            int iteration = currentIteration.incrementAndGet();
            notifyProgress("Iteration " + iteration + ": Current coverage " + 
                String.format("%.1f%%", currentCoverage.overallLineCoverage() * 100));
            
            List<CoverageInfo> uncoveredInfo = getUncoveredInfo(currentCoverage, classInfo);
            
            if (uncoveredInfo.isEmpty()) {
                notifyProgress("No more uncovered code to improve");
                break;
            }
            
            String additionalTests = testGenerator.generateAdditionalTests(classInfo, uncoveredInfo);
            
            if (additionalTests != null && !additionalTests.isEmpty()) {
                appendTestsToFile(testFile, additionalTests);
                notifyProgress("Added additional tests for uncovered code");
            }
            
            currentCoverage = runTestsAndGetCoverage();
            result.addCoverageReport(currentIteration.get(), currentCoverage);
            notifyCoverage(currentCoverage);
        }
        
        result.setFinalCoverage(currentCoverage);
        result.setSuccess(meetsTarget(currentCoverage));
        result.setIterations(currentIteration.get());
        
        if (result.isSuccess()) {
            notifyProgress("Target coverage achieved: " + 
                String.format("%.1f%%", currentCoverage.overallLineCoverage() * 100));
        } else {
            notifyProgress("Max iterations reached. Final coverage: " + 
                String.format("%.1f%%", currentCoverage.overallLineCoverage() * 100));
        }
        
        return result;
    }

    public List<OptimizationResult> optimizeDirectory(File sourceDirectory) {
        List<OptimizationResult> results = new ArrayList<>();
        
        List<File> javaFiles = findJavaFiles(sourceDirectory);
        notifyProgress("Found " + javaFiles.size() + " Java files to process");
        
        for (File javaFile : javaFiles) {
            try {
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
}
