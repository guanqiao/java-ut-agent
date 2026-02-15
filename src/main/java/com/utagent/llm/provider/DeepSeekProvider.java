package com.utagent.llm.provider;

import com.utagent.llm.ChatRequest;
import com.utagent.llm.ChatResponse;
import com.utagent.llm.TokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class DeepSeekProvider extends AbstractLLMProvider {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final String CHAT_ENDPOINT = "/chat/completions";

    public DeepSeekProvider(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL);
    }

    public DeepSeekProvider(String apiKey, String baseUrl, String model) {
        this(apiKey, baseUrl, model, null);
    }

    public DeepSeekProvider(String apiKey, String baseUrl, String model, String caCertPath) {
        super(apiKey, 
              baseUrl != null ? baseUrl : DEFAULT_BASE_URL, 
              model != null ? model : DEFAULT_MODEL,
              caCertPath);
    }

    @Override
    public String name() {
        return "DeepSeek";
    }

    @Override
    protected String getChatEndpoint() {
        return baseUrl + CHAT_ENDPOINT;
    }

    @Override
    protected ObjectNode buildRequestBody(ChatRequest request) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", request.model() != null ? request.model() : defaultModel);
        requestBody.put("temperature", request.temperature());
        requestBody.put("max_tokens", request.maxTokens());
        requestBody.put("stream", request.stream());
        
        requestBody.set("messages", buildMessagesArray(request));
        
        return requestBody;
    }

    @Override
    protected ChatResponse parseResponse(String responseBody) throws IOException {
        JsonNode responseJson = objectMapper.readTree(responseBody);
        
        String content = responseJson.path("choices")
            .path(0)
            .path("message")
            .path("content")
            .asText();
        
        JsonNode usageNode = responseJson.path("usage");
        TokenUsage tokenUsage = new TokenUsage(
            usageNode.path("prompt_tokens").asInt(0),
            usageNode.path("completion_tokens").asInt(0),
            usageNode.path("total_tokens").asInt(0)
        );
        lastTokenUsage.set(tokenUsage);
        
        String model = responseJson.path("model").asText();
        String finishReason = responseJson.path("choices")
            .path(0)
            .path("finish_reason")
            .asText();
        
        return ChatResponse.builder()
            .content(content)
            .tokenUsage(tokenUsage)
            .model(model)
            .finishReason(finishReason)
            .success(true)
            .build();
    }

    @Override
    protected String extractStreamContent(String data) {
        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            JsonNode delta = jsonNode.path("choices").path(0).path("delta");
            return delta.path("content").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
