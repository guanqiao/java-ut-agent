package com.utagent.service;

import com.utagent.config.AgentConfig;
import com.utagent.generator.TestGenerator;
import com.utagent.model.ClassInfo;
import com.utagent.parser.FrameworkDetector;
import com.utagent.parser.FrameworkType;
import com.utagent.parser.JavaCodeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;
import java.util.Set;

public class TestGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(TestGenerationService.class);

    private final AgentConfig config;
    private final JavaCodeParser codeParser;
    private final TestGenerator testGenerator;
    private final FrameworkDetector frameworkDetector;

    public TestGenerationService(AgentConfig config) {
        this(config, new JavaCodeParser(), new TestGenerator(), new FrameworkDetector());
    }

    public TestGenerationService(AgentConfig config,
                                  JavaCodeParser codeParser,
                                  TestGenerator testGenerator,
                                  FrameworkDetector frameworkDetector) {
        this.config = config;
        this.codeParser = codeParser;
        this.testGenerator = testGenerator;
        this.frameworkDetector = frameworkDetector;
    }

    public double getTargetCoverage() {
        return config.getCoverage().getTargetOrDefault();
    }

    public int getMaxIterations() {
        return config.getCoverage().getMaxIterationsOrDefault();
    }

    public boolean isVerbose() {
        return config.getOutput().getVerboseOrDefault();
    }

    public Optional<ClassInfo> parseSource(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) {
            return Optional.empty();
        }
        
        var result = codeParser.parseFile(sourceFile);
        return result.isPresent() ? Optional.of(result.get()) : Optional.empty();
    }

    public Set<FrameworkType> detectFrameworks(ClassInfo classInfo) {
        return frameworkDetector.detectFrameworks(classInfo);
    }

    public GenerationResult generateTests(ClassInfo classInfo) {
        try {
            logger.info("Generating tests for class: {}", classInfo.className());
            
            String testCode = testGenerator.generateTestClass(classInfo);
            
            if (testCode == null || testCode.isEmpty()) {
                return GenerationResult.failure("Failed to generate test code");
            }
            
            return GenerationResult.success(testCode, 0, 0.0);
        } catch (Exception e) {
            logger.error("Error generating tests for class: {}", classInfo.className(), e);
            return GenerationResult.failure(e.getMessage());
        }
    }

    public GenerationResult generateTests(File sourceFile) {
        Optional<ClassInfo> classInfo = parseSource(sourceFile);
        
        if (classInfo.isEmpty()) {
            return GenerationResult.failure("Failed to parse source file: " + sourceFile.getAbsolutePath());
        }
        
        return generateTests(classInfo.get());
    }

    public String getProviderName() {
        return testGenerator.getProviderName();
    }

    public boolean isAIEnabled() {
        return testGenerator.isAIEnabled();
    }
}
