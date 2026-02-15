package com.utagent.llm.provider;

import com.utagent.llm.ChatRequest;
import com.utagent.llm.ChatResponse;
import com.utagent.llm.TokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Request;

import java.io.IOException;

public class ClaudeProvider extends AbstractLLMProvider {

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final String DEFAULT_MODEL = "claude-3-sonnet-20240229";
    private static final String CHAT_ENDPOINT = "/messages";
    private static final String API_VERSION = "2024-01-01";

    public ClaudeProvider(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL);
    }

    public ClaudeProvider(String apiKey, String baseUrl, String model) {
        this(apiKey, baseUrl, model, null);
    }

    public ClaudeProvider(String apiKey, String baseUrl, String model, String caCertPath) {
        super(apiKey, 
              baseUrl != null ? baseUrl : DEFAULT_BASE_URL, 
              model != null ? model : DEFAULT_MODEL,
              caCertPath);
    }

    @Override
    public String name() {
        return "Claude";
    }

    @Override
    protected String getChatEndpoint() {
        return baseUrl + CHAT_ENDPOINT;
    }

    @Override
    protected Request.Builder addAuthHeaders(Request.Builder builder) {
        return builder
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", API_VERSION);
    }

    @Override
    protected ObjectNode buildRequestBody(ChatRequest request) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", request.model() != null ? request.model() : defaultModel);
        requestBody.put("max_tokens", request.maxTokens());
        requestBody.put("stream", request.stream());
        
        if (request.temperature() != 0.7) {
            requestBody.put("temperature", request.temperature());
        }
        
        String systemContent = null;
        ArrayNode messagesArray = objectMapper.createArrayNode();
        
        for (var msg : request.messages()) {
            if ("system".equals(msg.role())) {
                systemContent = msg.content();
            } else {
                ObjectNode msgNode = messagesArray.addObject();
                msgNode.put("role", msg.role());
                ObjectNode contentNode = msgNode.putArray("content").addObject();
                contentNode.put("type", "text");
                contentNode.put("text", msg.content());
            }
        }
        
        if (systemContent != null) {
            requestBody.put("system", systemContent);
        }
        requestBody.set("messages", messagesArray);
        
        return requestBody;
    }

    @Override
    protected ChatResponse parseResponse(String responseBody) throws IOException {
        JsonNode responseJson = objectMapper.readTree(responseBody);
        
        String type = responseJson.path("type").asText();
        if ("error".equals(type)) {
            String errorMsg = responseJson.path("error").path("message").asText();
            return ChatResponse.error(errorMsg);
        }
        
        StringBuilder content = new StringBuilder();
        JsonNode contentArray = responseJson.path("content");
        if (contentArray.isArray()) {
            for (JsonNode node : contentArray) {
                if ("text".equals(node.path("type").asText())) {
                    content.append(node.path("text").asText());
                }
            }
        }
        
        JsonNode usageNode = responseJson.path("usage");
        TokenUsage tokenUsage = new TokenUsage(
            usageNode.path("input_tokens").asInt(0),
            usageNode.path("output_tokens").asInt(0),
            usageNode.path("input_tokens").asInt(0) + usageNode.path("output_tokens").asInt(0)
        );
        lastTokenUsage.set(tokenUsage);
        
        String model = responseJson.path("model").asText();
        String stopReason = responseJson.path("stop_reason").asText();
        
        return ChatResponse.builder()
            .content(content.toString())
            .tokenUsage(tokenUsage)
            .model(model)
            .finishReason(stopReason)
            .success(true)
            .build();
    }

    @Override
    protected String extractStreamContent(String data) {
        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            String type = jsonNode.path("type").asText();
            
            if ("content_block_delta".equals(type)) {
                return jsonNode.path("delta").path("text").asText("");
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
