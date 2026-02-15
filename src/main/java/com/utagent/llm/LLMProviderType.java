package com.utagent.llm;

public enum LLMProviderType {
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1"),
    CLAUDE("claude", "Anthropic Claude", "https://api.anthropic.com/v1"),
    OLLAMA("ollama", "Ollama (Local)", "http://localhost:11434/api"),
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com/v1");
    
    private final String id;
    private final String displayName;
    private final String defaultBaseUrl;
    
    LLMProviderType(String id, String displayName, String defaultBaseUrl) {
        this.id = id;
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }
    
    public static LLMProviderType fromId(String id) {
        for (LLMProviderType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return OPENAI;
    }
}
