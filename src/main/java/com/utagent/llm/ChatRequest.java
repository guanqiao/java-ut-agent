package com.utagent.llm;

import java.util.ArrayList;
import java.util.List;

public class ChatRequest {
    
    private final List<Message> messages;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final boolean stream;
    
    private ChatRequest(Builder builder) {
        this.messages = List.copyOf(builder.messages);
        this.model = builder.model;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.stream = builder.stream;
    }
    
    public List<Message> messages() {
        return messages;
    }
    
    public String model() {
        return model;
    }
    
    public double temperature() {
        return temperature;
    }
    
    public int maxTokens() {
        return maxTokens;
    }
    
    public boolean stream() {
        return stream;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public ChatRequest withStream(boolean stream) {
        return new Builder()
            .messages(messages)
            .model(model)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .stream(stream)
            .build();
    }
    
    public static class Builder {
        private List<Message> messages = new ArrayList<>();
        private String model = "gpt-4";
        private double temperature = 0.7;
        private int maxTokens = 4096;
        private boolean stream = false;
        
        public Builder messages(List<Message> messages) {
            this.messages = new ArrayList<>(messages);
            return this;
        }
        
        public Builder addMessage(Message message) {
            this.messages.add(message);
            return this;
        }
        
        public Builder systemPrompt(String content) {
            this.messages.add(0, Message.system(content));
            return this;
        }
        
        public Builder userMessage(String content) {
            this.messages.add(Message.user(content));
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }
        
        public ChatRequest build() {
            return new ChatRequest(this);
        }
    }
}
