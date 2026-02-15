package com.utagent.llm;

public class ChatResponse {
    
    private final String content;
    private final TokenUsage tokenUsage;
    private final String model;
    private final String finishReason;
    private final boolean success;
    private final String errorMessage;
    
    private ChatResponse(Builder builder) {
        this.content = builder.content;
        this.tokenUsage = builder.tokenUsage;
        this.model = builder.model;
        this.finishReason = builder.finishReason;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }
    
    public String content() {
        return content;
    }
    
    public TokenUsage tokenUsage() {
        return tokenUsage;
    }
    
    public String model() {
        return model;
    }
    
    public String finishReason() {
        return finishReason;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String errorMessage() {
        return errorMessage;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static ChatResponse success(String content) {
        return builder()
            .content(content)
            .success(true)
            .tokenUsage(TokenUsage.empty())
            .build();
    }
    
    public static ChatResponse error(String errorMessage) {
        return builder()
            .success(false)
            .errorMessage(errorMessage)
            .tokenUsage(TokenUsage.empty())
            .build();
    }
    
    public static class Builder {
        private String content = "";
        private TokenUsage tokenUsage = TokenUsage.empty();
        private String model;
        private String finishReason;
        private boolean success = true;
        private String errorMessage;
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder tokenUsage(TokenUsage tokenUsage) {
            this.tokenUsage = tokenUsage;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder finishReason(String finishReason) {
            this.finishReason = finishReason;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public ChatResponse build() {
            return new ChatResponse(this);
        }
    }
}
