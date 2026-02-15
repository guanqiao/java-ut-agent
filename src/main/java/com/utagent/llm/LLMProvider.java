package com.utagent.llm;

import java.util.function.Consumer;

public interface LLMProvider {
    
    String name();
    
    ChatResponse chat(ChatRequest request);
    
    void chatStream(ChatRequest request, Consumer<String> chunkConsumer, Consumer<ChatResponse> completeConsumer);
    
    TokenUsage getLastTokenUsage();
    
    boolean supportsStreaming();
    
    default boolean isAvailable() {
        return true;
    }
    
    default ChatResponse chatWithRetry(ChatRequest request, int maxRetries) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return chat(request);
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return ChatResponse.error("Failed after " + maxRetries + " attempts: " + 
            (lastException != null ? lastException.getMessage() : "Unknown error"));
    }
}
