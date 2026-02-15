package com.utagent.llm;

public record LLMConfig(
    String provider,
    String apiKey,
    String baseUrl,
    String model,
    Double temperature,
    Integer maxTokens,
    Integer maxRetries,
    String caCertPath
) {
    public static final String DEFAULT_PROVIDER = "openai";
    public static final String DEFAULT_MODEL = "gpt-4";
    public static final double DEFAULT_TEMPERATURE = 0.7;
    public static final int DEFAULT_MAX_TOKENS = 4096;
    public static final int DEFAULT_MAX_RETRIES = 3;
    
    public static LLMConfig defaults() {
        return new LLMConfig(
            DEFAULT_PROVIDER,
            null,
            null,
            DEFAULT_MODEL,
            DEFAULT_TEMPERATURE,
            DEFAULT_MAX_TOKENS,
            DEFAULT_MAX_RETRIES,
            null
        );
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public LLMConfig withApiKey(String apiKey) {
        return new LLMConfig(provider, apiKey, baseUrl, model, temperature, maxTokens, maxRetries, caCertPath);
    }
    
    public LLMProviderType getProviderType() {
        return LLMProviderType.fromId(provider);
    }
    
    public double getTemperatureOrDefault() {
        return temperature != null ? temperature : DEFAULT_TEMPERATURE;
    }
    
    public int getMaxTokensOrDefault() {
        return maxTokens != null ? maxTokens : DEFAULT_MAX_TOKENS;
    }
    
    public int getMaxRetriesOrDefault() {
        return maxRetries != null ? maxRetries : DEFAULT_MAX_RETRIES;
    }
    
    public String getModelOrDefault() {
        return model != null ? model : DEFAULT_MODEL;
    }
    
    public static class Builder {
        private String provider = DEFAULT_PROVIDER;
        private String apiKey;
        private String baseUrl;
        private String model = DEFAULT_MODEL;
        private Double temperature = DEFAULT_TEMPERATURE;
        private Integer maxTokens = DEFAULT_MAX_TOKENS;
        private Integer maxRetries = DEFAULT_MAX_RETRIES;
        private String caCertPath;
        
        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }
        
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }
        
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder caCertPath(String caCertPath) {
            this.caCertPath = caCertPath;
            return this;
        }
        
        public LLMConfig build() {
            return new LLMConfig(provider, apiKey, baseUrl, model, temperature, maxTokens, maxRetries, caCertPath);
        }
    }
}
