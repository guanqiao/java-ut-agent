package com.utagent.llm;

import com.utagent.llm.provider.ClaudeProvider;
import com.utagent.llm.provider.DeepSeekProvider;
import com.utagent.llm.provider.OllamaProvider;
import com.utagent.llm.provider.OpenAIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public final class LLMProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(LLMProviderFactory.class);
    
    private static final Map<LLMProviderType, LLMProvider> PROVIDERS = new HashMap<>();
    
    static {
        registerDefaultProviders();
    }
    
    private LLMProviderFactory() {
    }
    
    private static void registerDefaultProviders() {
        ServiceLoader.load(LLMProvider.class).forEach(provider -> {
            logger.info("Discovered LLM provider: {}", provider.name());
        });
    }
    
    public static LLMProvider create(LLMProviderType type, String apiKey) {
        return create(type, apiKey, null, null);
    }
    
    public static LLMProvider create(LLMProviderType type, String apiKey, String baseUrl, String model) {
        return create(type, apiKey, baseUrl, model, null);
    }

    public static LLMProvider create(LLMProviderType type, String apiKey, String baseUrl, String model, String caCertPath) {
        logger.debug("Creating LLM provider: {} with baseUrl={}, model={}, caCertPath={}", type, baseUrl, model, caCertPath);
        
        return switch (type) {
            case OPENAI -> new OpenAIProvider(apiKey, baseUrl, model, caCertPath);
            case CLAUDE -> new ClaudeProvider(apiKey, baseUrl, model, caCertPath);
            case OLLAMA -> new OllamaProvider(baseUrl, model, caCertPath);
            case DEEPSEEK -> new DeepSeekProvider(apiKey, baseUrl, model, caCertPath);
        };
    }
    
    public static LLMProvider create(LLMConfig config) {
        LLMProviderType type = LLMProviderType.fromId(config.provider());
        return create(type, config.apiKey(), config.baseUrl(), config.model(), config.caCertPath());
    }
    
    public static LLMProvider createFromEnv(LLMProviderType type) {
        String apiKey = getApiKeyFromEnv(type);
        return create(type, apiKey);
    }
    
    private static String getApiKeyFromEnv(LLMProviderType type) {
        return switch (type) {
            case OPENAI -> System.getenv("OPENAI_API_KEY");
            case CLAUDE -> System.getenv("ANTHROPIC_API_KEY");
            case DEEPSEEK -> System.getenv("DEEPSEEK_API_KEY");
            case OLLAMA -> null;
        };
    }
    
    public static Set<String> getAvailableProviders() {
        return java.util.Arrays.stream(LLMProviderType.values())
            .map(LLMProviderType::getId)
            .collect(Collectors.toSet());
    }
    
    public static boolean isProviderAvailable(LLMProviderType type) {
        String apiKey = getApiKeyFromEnv(type);
        return type == LLMProviderType.OLLAMA || (apiKey != null && !apiKey.isEmpty());
    }
}
