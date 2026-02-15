package com.utagent.llm;

public record TokenUsage(
    int promptTokens,
    int completionTokens,
    int totalTokens
) {
    public static TokenUsage empty() {
        return new TokenUsage(0, 0, 0);
    }
    
    public TokenUsage add(TokenUsage other) {
        return new TokenUsage(
            this.promptTokens + other.promptTokens,
            this.completionTokens + other.completionTokens,
            this.totalTokens + other.totalTokens
        );
    }
    
    public double estimatedCost(double promptPrice, double completionPrice) {
        return (promptTokens * promptPrice / 1000.0) + 
               (completionTokens * completionPrice / 1000.0);
    }
}
