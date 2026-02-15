package com.utagent.generator;

import com.utagent.generator.llm.LLMClient;
import com.utagent.generator.llm.PromptBuilder;
import com.utagent.generator.strategy.MyBatisPlusTestStrategy;
import com.utagent.generator.strategy.MyBatisTestStrategy;
import com.utagent.generator.strategy.SpringBootTestStrategy;
import com.utagent.generator.strategy.SpringMvcTestStrategy;
import com.utagent.generator.strategy.TestGenerationStrategy;
import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.parser.FrameworkDetector;
import com.utagent.parser.FrameworkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TestGenerator.class);

    private final LLMClient llmClient;
    private final PromptBuilder promptBuilder;
    private final FrameworkDetector frameworkDetector;
    private final Map<FrameworkType, TestGenerationStrategy> strategies;
    private final boolean useAI;

    public TestGenerator() {
        this(null);
    }

    public TestGenerator(String apiKey) {
        this.llmClient = apiKey != null ? new LLMClient(apiKey) : null;
        this.promptBuilder = new PromptBuilder();
        this.frameworkDetector = new FrameworkDetector();
        this.strategies = new EnumMap<>(FrameworkType.class);
        this.useAI = apiKey != null;
        
        initializeStrategies();
    }

    private void initializeStrategies() {
        strategies.put(FrameworkType.SPRING_MVC, new SpringMvcTestStrategy());
        strategies.put(FrameworkType.SPRING_BOOT, new SpringBootTestStrategy());
        strategies.put(FrameworkType.MYBATIS, new MyBatisTestStrategy());
        strategies.put(FrameworkType.MYBATIS_PLUS, new MyBatisPlusTestStrategy());
    }

    public String generateTestClass(ClassInfo classInfo) {
        Set<FrameworkType> frameworks = frameworkDetector.detectFrameworks(classInfo);
        
        if (useAI && llmClient != null) {
            return generateWithAI(classInfo, frameworks);
        } else {
            return generateWithStrategy(classInfo, frameworks);
        }
    }

    public String generateAdditionalTests(ClassInfo classInfo, List<CoverageInfo> coverageInfo) {
        if (useAI && llmClient != null) {
            return generateAdditionalTestsWithAI(classInfo, coverageInfo);
        } else {
            return generateAdditionalTestsWithStrategy(classInfo, coverageInfo);
        }
    }

    private String generateWithAI(ClassInfo classInfo, Set<FrameworkType> frameworks) {
        logger.info("Generating tests for {} using AI", classInfo.className());
        
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildTestGenerationPrompt(classInfo, frameworks);
        
        try {
            String response = llmClient.chatWithRetry(systemPrompt, userPrompt, 3);
            return extractCodeFromResponse(response);
        } catch (Exception e) {
            logger.warn("AI generation failed, falling back to strategy-based generation", e);
            return generateWithStrategy(classInfo, frameworks);
        }
    }

    private String generateWithStrategy(ClassInfo classInfo, Set<FrameworkType> frameworks) {
        logger.info("Generating tests for {} using strategy pattern", classInfo.className());
        
        TestGenerationStrategy strategy = selectStrategy(frameworks);
        return strategy.generateTestClass(classInfo);
    }

    private TestGenerationStrategy selectStrategy(Set<FrameworkType> frameworks) {
        if (frameworks.contains(FrameworkType.MYBATIS_PLUS)) {
            return strategies.get(FrameworkType.MYBATIS_PLUS);
        }
        if (frameworks.contains(FrameworkType.SPRING_BOOT)) {
            return strategies.get(FrameworkType.SPRING_BOOT);
        }
        if (frameworks.contains(FrameworkType.MYBATIS)) {
            return strategies.get(FrameworkType.MYBATIS);
        }
        if (frameworks.contains(FrameworkType.SPRING_MVC)) {
            return strategies.get(FrameworkType.SPRING_MVC);
        }
        
        return strategies.get(FrameworkType.SPRING_MVC);
    }

    private String generateAdditionalTestsWithAI(ClassInfo classInfo, List<CoverageInfo> coverageInfo) {
        logger.info("Generating additional tests for {} using AI", classInfo.className());
        
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildCoverageImprovementPrompt(classInfo, coverageInfo);
        
        try {
            String response = llmClient.chatWithRetry(systemPrompt, userPrompt, 3);
            return extractCodeFromResponse(response);
        } catch (Exception e) {
            logger.warn("AI generation failed, falling back to strategy-based generation", e);
            return generateAdditionalTestsWithStrategy(classInfo, coverageInfo);
        }
    }

    private String generateAdditionalTestsWithStrategy(ClassInfo classInfo, List<CoverageInfo> coverageInfo) {
        Set<FrameworkType> frameworks = frameworkDetector.detectFrameworks(classInfo);
        TestGenerationStrategy strategy = selectStrategy(frameworks);
        return strategy.generateAdditionalTests(classInfo, coverageInfo);
    }

    private String extractCodeFromResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }
        
        String code = response;
        
        if (code.contains("```java")) {
            int start = code.indexOf("```java") + 7;
            int end = code.indexOf("```", start);
            if (end > start) {
                code = code.substring(start, end).trim();
            }
        } else if (code.contains("```")) {
            int start = code.indexOf("```") + 3;
            int end = code.indexOf("```", start);
            if (end > start) {
                code = code.substring(start, end).trim();
            }
        }
        
        return code;
    }

    public void setApiKey(String apiKey) {
        if (llmClient != null) {
            throw new IllegalStateException("LLM client already initialized");
        }
    }

    public boolean isAIEnabled() {
        return useAI;
    }
}
