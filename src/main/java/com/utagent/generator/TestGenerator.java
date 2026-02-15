package com.utagent.generator;

import com.utagent.generator.llm.PromptBuilder;
import com.utagent.generator.strategy.MyBatisPlusTestStrategy;
import com.utagent.generator.strategy.MyBatisTestStrategy;
import com.utagent.generator.strategy.SpringBootTestStrategy;
import com.utagent.generator.strategy.SpringMvcTestStrategy;
import com.utagent.generator.strategy.TestGenerationStrategy;
import com.utagent.llm.ChatRequest;
import com.utagent.llm.ChatResponse;
import com.utagent.llm.LLMConfig;
import com.utagent.llm.LLMProvider;
import com.utagent.llm.LLMProviderFactory;
import com.utagent.llm.LLMProviderType;
import com.utagent.llm.Message;
import com.utagent.llm.TokenUsage;
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
import java.util.concurrent.atomic.AtomicReference;

public class TestGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TestGenerator.class);

    private final LLMProvider llmProvider;
    private final PromptBuilder promptBuilder;
    private final FrameworkDetector frameworkDetector;
    private final Map<FrameworkType, TestGenerationStrategy> strategies;
    private final boolean useAI;
    private final AtomicReference<TokenUsage> totalTokenUsage = new AtomicReference<>(TokenUsage.empty());

    public TestGenerator() {
        this(null, null, null, null);
    }

    public TestGenerator(String apiKey) {
        this(apiKey, LLMConfig.DEFAULT_PROVIDER, null, null);
    }

    public TestGenerator(String apiKey, String provider, String baseUrl, String model) {
        this.llmProvider = createProvider(apiKey, provider, baseUrl, model);
        this.promptBuilder = new PromptBuilder();
        this.frameworkDetector = new FrameworkDetector();
        this.strategies = new EnumMap<>(FrameworkType.class);
        this.useAI = llmProvider != null && llmProvider.isAvailable();
        
        initializeStrategies();
    }
    
    public static TestGenerator fromConfig(LLMConfig config) {
        return new TestGenerator(
            config.apiKey(),
            config.provider(),
            config.baseUrl(),
            config.model()
        );
    }

    private LLMProvider createProvider(String apiKey, String provider, String baseUrl, String model) {
        if (apiKey == null && !"ollama".equalsIgnoreCase(provider)) {
            String envKey = switch (provider != null ? provider.toLowerCase() : "openai") {
                case "claude" -> System.getenv("ANTHROPIC_API_KEY");
                case "deepseek" -> System.getenv("DEEPSEEK_API_KEY");
                default -> System.getenv("OPENAI_API_KEY");
            };
            apiKey = envKey;
        }
        
        if (apiKey == null && !"ollama".equalsIgnoreCase(provider)) {
            logger.info("No API key provided, using template-based generation");
            return null;
        }
        
        LLMProviderType providerType = LLMProviderType.fromId(provider);
        return LLMProviderFactory.create(providerType, apiKey, baseUrl, model);
    }

    private void initializeStrategies() {
        strategies.put(FrameworkType.SPRING_MVC, new SpringMvcTestStrategy());
        strategies.put(FrameworkType.SPRING_BOOT, new SpringBootTestStrategy());
        strategies.put(FrameworkType.MYBATIS, new MyBatisTestStrategy());
        strategies.put(FrameworkType.MYBATIS_PLUS, new MyBatisPlusTestStrategy());
    }

    public String generateTestClass(ClassInfo classInfo) {
        Set<FrameworkType> frameworks = frameworkDetector.detectFrameworks(classInfo);
        
        if (useAI && llmProvider != null) {
            return generateWithAI(classInfo, frameworks);
        } else {
            return generateWithStrategy(classInfo, frameworks);
        }
    }

    public String generateAdditionalTests(ClassInfo classInfo, List<CoverageInfo> coverageInfo) {
        if (useAI && llmProvider != null) {
            return generateAdditionalTestsWithAI(classInfo, coverageInfo);
        } else {
            return generateAdditionalTestsWithStrategy(classInfo, coverageInfo);
        }
    }

    private String generateWithAI(ClassInfo classInfo, Set<FrameworkType> frameworks) {
        logger.info("Generating tests for {} using AI ({})", classInfo.className(), llmProvider.name());
        
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildTestGenerationPrompt(classInfo, frameworks);
        
        ChatRequest request = ChatRequest.builder()
            .systemPrompt(systemPrompt)
            .userMessage(userPrompt)
            .build();
        
        ChatResponse response = llmProvider.chatWithRetry(request, 3);
        
        if (response.isSuccess()) {
            updateTokenUsage(response.tokenUsage());
            return extractCodeFromResponse(response.content());
        } else {
            logger.warn("AI generation failed: {}, falling back to strategy-based generation", 
                response.errorMessage());
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
        
        ChatRequest request = ChatRequest.builder()
            .systemPrompt(systemPrompt)
            .userMessage(userPrompt)
            .build();
        
        ChatResponse response = llmProvider.chatWithRetry(request, 3);
        
        if (response.isSuccess()) {
            updateTokenUsage(response.tokenUsage());
            return extractCodeFromResponse(response.content());
        } else {
            logger.warn("AI generation failed: {}, falling back to strategy-based generation", 
                response.errorMessage());
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

    private void updateTokenUsage(TokenUsage usage) {
        if (usage != null) {
            totalTokenUsage.updateAndGet(current -> current.add(usage));
        }
    }

    public TokenUsage getTotalTokenUsage() {
        return totalTokenUsage.get();
    }

    public boolean isAIEnabled() {
        return useAI;
    }
    
    public String getProviderName() {
        return llmProvider != null ? llmProvider.name() : "Template";
    }
}
