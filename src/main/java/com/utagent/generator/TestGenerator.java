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
import com.utagent.model.MethodInfo;
import com.utagent.model.ParsedTestFile;
import com.utagent.monitoring.LLMCallMonitor;
import com.utagent.parser.FrameworkDetector;
import com.utagent.parser.FrameworkType;
import com.utagent.util.ApiKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
        
        LLMCallMonitor monitor = LLMCallMonitor.getInstance();
        LLMCallMonitor.CallRecord callRecord = monitor.startCall(
            llmProvider.name(),
            llmProvider instanceof com.utagent.llm.provider.AbstractLLMProvider ? 
                ((com.utagent.llm.provider.AbstractLLMProvider) llmProvider).getModel() : "unknown",
            "test_generation"
        );
        
        try {
            ChatResponse response = llmProvider.chatWithRetry(request, 3);
            
            if (response.isSuccess()) {
                updateTokenUsage(response.tokenUsage());
                monitor.endCall(callRecord, response.tokenUsage(), 
                    truncatePreview(response.content(), 100));
                return extractCodeFromResponse(response.content());
            } else {
                monitor.failCall(callRecord, response.errorMessage());
                logger.warn("AI generation failed: {}, falling back to strategy-based generation", 
                    response.errorMessage());
                return generateWithStrategy(classInfo, frameworks);
            }
        } catch (Exception e) {
            monitor.failCall(callRecord, e.getMessage());
            logger.warn("AI generation exception: {}, falling back to strategy-based generation", 
                e.getMessage());
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
        
        LLMCallMonitor monitor = LLMCallMonitor.getInstance();
        LLMCallMonitor.CallRecord callRecord = monitor.startCall(
            llmProvider.name(),
            llmProvider instanceof com.utagent.llm.provider.AbstractLLMProvider ? 
                ((com.utagent.llm.provider.AbstractLLMProvider) llmProvider).getModel() : "unknown",
            "coverage_improvement"
        );
        
        try {
            ChatResponse response = llmProvider.chatWithRetry(request, 3);
            
            if (response.isSuccess()) {
                updateTokenUsage(response.tokenUsage());
                monitor.endCall(callRecord, response.tokenUsage(), 
                    truncatePreview(response.content(), 100));
                return extractCodeFromResponse(response.content());
            } else {
                monitor.failCall(callRecord, response.errorMessage());
                logger.warn("AI generation failed: {}, falling back to strategy-based generation", 
                    response.errorMessage());
                return generateAdditionalTestsWithStrategy(classInfo, coverageInfo);
            }
        } catch (Exception e) {
            monitor.failCall(callRecord, e.getMessage());
            logger.warn("AI generation exception: {}, falling back to strategy-based generation", 
                e.getMessage());
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
    
    private String truncatePreview(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
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

    public String generateIncrementalTests(ClassInfo classInfo, 
                                           ParsedTestFile existingTests,
                                           List<CoverageInfo> uncoveredInfo) {
        Set<FrameworkType> frameworks = frameworkDetector.detectFrameworks(classInfo);
        
        Set<String> testedMethods = existingTests != null ? 
            existingTests.testedMethods() : new HashSet<>();
        
        List<MethodInfo> untestedMethods = classInfo.methods().stream()
            .filter(m -> !m.isPrivate() && !m.isAbstract())
            .filter(m -> !isMethodTested(m.name(), testedMethods))
            .collect(Collectors.toList());
        
        if (untestedMethods.isEmpty() && (uncoveredInfo == null || uncoveredInfo.isEmpty())) {
            logger.info("All methods already have tests for {}", classInfo.className());
            return "";
        }
        
        if (useAI && llmProvider != null) {
            return generateIncrementalWithAI(classInfo, frameworks, existingTests, 
                                            untestedMethods, uncoveredInfo);
        } else {
            return generateIncrementalWithStrategy(classInfo, frameworks, existingTests, 
                                                   untestedMethods, uncoveredInfo);
        }
    }

    private boolean isMethodTested(String methodName, Set<String> testedMethods) {
        String lowerName = methodName.toLowerCase();
        for (String tested : testedMethods) {
            if (tested != null && tested.toLowerCase().equals(lowerName)) {
                return true;
            }
        }
        return false;
    }

    private String generateIncrementalWithAI(ClassInfo classInfo, 
                                             Set<FrameworkType> frameworks,
                                             ParsedTestFile existingTests,
                                             List<MethodInfo> untestedMethods,
                                             List<CoverageInfo> uncoveredInfo) {
        logger.info("Generating incremental tests for {} using AI", classInfo.className());
        
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildIncrementalTestPrompt(
            classInfo, frameworks, existingTests, untestedMethods, uncoveredInfo);
        
        ChatRequest request = ChatRequest.builder()
            .systemPrompt(systemPrompt)
            .userMessage(userPrompt)
            .build();
        
        LLMCallMonitor monitor = LLMCallMonitor.getInstance();
        LLMCallMonitor.CallRecord callRecord = monitor.startCall(
            llmProvider.name(),
            llmProvider instanceof com.utagent.llm.provider.AbstractLLMProvider ? 
                ((com.utagent.llm.provider.AbstractLLMProvider) llmProvider).getModel() : "unknown",
            "incremental_test_generation"
        );
        
        try {
            ChatResponse response = llmProvider.chatWithRetry(request, 3);
            
            if (response.isSuccess()) {
                updateTokenUsage(response.tokenUsage());
                monitor.endCall(callRecord, response.tokenUsage(), 
                    truncatePreview(response.content(), 100));
                return extractCodeFromResponse(response.content());
            } else {
                monitor.failCall(callRecord, response.errorMessage());
                logger.warn("AI incremental generation failed: {}, falling back to strategy", 
                    response.errorMessage());
                return generateIncrementalWithStrategy(classInfo, frameworks, existingTests, 
                                                       untestedMethods, uncoveredInfo);
            }
        } catch (Exception e) {
            monitor.failCall(callRecord, e.getMessage());
            logger.warn("AI incremental generation exception: {}, falling back to strategy", 
                e.getMessage());
            return generateIncrementalWithStrategy(classInfo, frameworks, existingTests, 
                                                   untestedMethods, uncoveredInfo);
        }
    }

    private String generateIncrementalWithStrategy(ClassInfo classInfo,
                                                    Set<FrameworkType> frameworks,
                                                    ParsedTestFile existingTests,
                                                    List<MethodInfo> untestedMethods,
                                                    List<CoverageInfo> uncoveredInfo) {
        logger.info("Generating incremental tests for {} using strategy pattern", 
                   classInfo.className());
        
        TestGenerationStrategy strategy = selectStrategy(frameworks);
        
        if (uncoveredInfo != null && !uncoveredInfo.isEmpty()) {
            return strategy.generateAdditionalTests(classInfo, uncoveredInfo);
        }
        
        return generateTestsForUntestedMethods(classInfo, untestedMethods, strategy);
    }

    private String generateTestsForUntestedMethods(ClassInfo classInfo, 
                                                    List<MethodInfo> untestedMethods,
                                                    TestGenerationStrategy strategy) {
        StringBuilder tests = new StringBuilder();
        
        for (MethodInfo method : untestedMethods) {
            String testCode = strategy.generateTestMethod(classInfo, method.name(), List.of());
            if (testCode != null && !testCode.isEmpty()) {
                tests.append(testCode).append("\n\n");
            }
        }
        
        return tests.toString().trim();
    }

    public String generateAdditionalTestsAvoidingDuplicates(ClassInfo classInfo,
                                                             List<CoverageInfo> coverageInfo,
                                                             Set<String> existingTestMethodNames) {
        if (useAI && llmProvider != null) {
            return generateAdditionalTestsAvoidingDuplicatesWithAI(classInfo, coverageInfo, 
                                                                   existingTestMethodNames);
        } else {
            return generateAdditionalTestsWithStrategy(classInfo, coverageInfo);
        }
    }

    private String generateAdditionalTestsAvoidingDuplicatesWithAI(ClassInfo classInfo,
                                                                     List<CoverageInfo> coverageInfo,
                                                                     Set<String> existingTestMethodNames) {
        logger.info("Generating additional tests for {} avoiding duplicates using AI", 
                   classInfo.className());
        
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildIncrementalCoveragePrompt(
            classInfo, coverageInfo, existingTestMethodNames);
        
        ChatRequest request = ChatRequest.builder()
            .systemPrompt(systemPrompt)
            .userMessage(userPrompt)
            .build();
        
        LLMCallMonitor monitor = LLMCallMonitor.getInstance();
        LLMCallMonitor.CallRecord callRecord = monitor.startCall(
            llmProvider.name(),
            llmProvider instanceof com.utagent.llm.provider.AbstractLLMProvider ? 
                ((com.utagent.llm.provider.AbstractLLMProvider) llmProvider).getModel() : "unknown",
            "coverage_improvement_incremental"
        );
        
        try {
            ChatResponse response = llmProvider.chatWithRetry(request, 3);
            
            if (response.isSuccess()) {
                updateTokenUsage(response.tokenUsage());
                monitor.endCall(callRecord, response.tokenUsage(), 
                    truncatePreview(response.content(), 100));
                return extractCodeFromResponse(response.content());
            } else {
                monitor.failCall(callRecord, response.errorMessage());
                logger.warn("AI generation failed: {}", response.errorMessage());
                return "";
            }
        } catch (Exception e) {
            monitor.failCall(callRecord, e.getMessage());
            logger.warn("AI generation exception: {}", e.getMessage());
            return "";
        }
    }
}
