package com.utagent.generator;

import com.utagent.generator.llm.PromptBuilder;
import com.utagent.generator.strategy.StrategyLoader;
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
import com.utagent.util.ApiKeyResolver;
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
    private final StrategyLoader strategyLoader;
    private final boolean useAI;
    private final AtomicReference<TokenUsage> totalTokenUsage = new AtomicReference<>(TokenUsage.empty());

    /**
     * 默认构造函数，使用模板生成（无AI）
     */
    public TestGenerator() {
        this(null, null, null, null, null, null);
    }

    /**
     * 使用API Key的便捷构造函数
     */
    public TestGenerator(String apiKey) {
        this(apiKey, LLMConfig.DEFAULT_PROVIDER, null, null, null, null);
    }

    /**
     * 完整参数构造函数
     */
    public TestGenerator(String apiKey, String provider, String baseUrl, String model) {
        this(apiKey, provider, baseUrl, model, null, null);
    }

    /**
     * 全依赖注入构造函数，便于测试和灵活配置
     */
    public TestGenerator(String apiKey,
                         String provider,
                         String baseUrl,
                         String model,
                         LLMProvider llmProvider,
                         Map<FrameworkType, TestGenerationStrategy> strategies) {
        if (llmProvider != null) {
            this.llmProvider = llmProvider;
        } else {
            this.llmProvider = createProvider(apiKey, provider, baseUrl, model);
        }
        this.promptBuilder = new PromptBuilder();
        this.frameworkDetector = new FrameworkDetector();
        this.useAI = this.llmProvider != null && this.llmProvider.isAvailable();

        // 使用 StrategyLoader 加载策略
        if (strategies != null && !strategies.isEmpty()) {
            this.strategyLoader = new StrategyLoader(false); // 不使用 SPI
            strategies.forEach(this.strategyLoader::registerStrategy);
        } else {
            this.strategyLoader = new StrategyLoader(true); // 使用 SPI
        }
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
        LLMProviderType providerType = LLMProviderType.fromId(provider);
        
        String resolvedApiKey = ApiKeyResolver.resolve(apiKey, providerType);

        if (resolvedApiKey == null && ApiKeyResolver.isApiKeyRequired(providerType)) {
            logger.info("No API key provided, using template-based generation");
            return null;
        }

        return LLMProviderFactory.create(providerType, resolvedApiKey, baseUrl, model);
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
            return strategyLoader.getStrategy(FrameworkType.MYBATIS_PLUS);
        }
        if (frameworks.contains(FrameworkType.SPRING_BOOT)) {
            return strategyLoader.getStrategy(FrameworkType.SPRING_BOOT);
        }
        if (frameworks.contains(FrameworkType.MYBATIS)) {
            return strategyLoader.getStrategy(FrameworkType.MYBATIS);
        }
        if (frameworks.contains(FrameworkType.SPRING_MVC)) {
            return strategyLoader.getStrategy(FrameworkType.SPRING_MVC);
        }

        return strategyLoader.getDefaultStrategy();
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
