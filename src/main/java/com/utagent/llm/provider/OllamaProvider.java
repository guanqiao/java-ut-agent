package com.utagent.llm.provider;

import com.utagent.llm.ChatRequest;
import com.utagent.llm.ChatResponse;
import com.utagent.llm.TokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Request;

import java.io.IOException;

public class OllamaProvider extends AbstractLLMProvider {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434/api";
    private static final String DEFAULT_MODEL = "llama3";
    private static final String CHAT_ENDPOINT = "/chat";

    public OllamaProvider() {
        this(DEFAULT_BASE_URL, DEFAULT_MODEL);
    }

    public OllamaProvider(String baseUrl, String model) {
        this(baseUrl, model, null);
    }

    public OllamaProvider(String baseUrl, String model, String caCertPath) {
        super(null, 
              baseUrl != null ? baseUrl : DEFAULT_BASE_URL, 
              model != null ? model : DEFAULT_MODEL,
              caCertPath);
    }

    @Override
    public String name() {
        return "Ollama";
    }

    @Override
    protected String getChatEndpoint() {
        return baseUrl + CHAT_ENDPOINT;
    }

    @Override
    protected Request.Builder addAuthHeaders(Request.Builder builder) {
        return builder;
    }

    @Override
    protected ObjectNode buildRequestBody(ChatRequest request) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", request.model() != null ? request.model() : defaultModel);
        requestBody.put("stream", request.stream());
        
        ObjectNode optionsNode = requestBody.putObject("options");
        optionsNode.put("temperature", request.temperature());
        optionsNode.put("num_predict", request.maxTokens());
        
        requestBody.set("messages", buildMessagesArray(request));
        
        return requestBody;
    }

    @Override
    protected ChatResponse parseResponse(String responseBody) throws IOException {
        JsonNode responseJson = objectMapper.readTree(responseBody);
        
        String content = responseJson.path("message")
            .path("content")
            .asText();
        
        JsonNode evalCount = responseJson.path("eval_count");
        JsonNode promptEvalCount = responseJson.path("prompt_eval_count");
        
        int promptTokens = promptEvalCount.isNumber() ? promptEvalCount.asInt() : 0;
        int completionTokens = evalCount.isNumber() ? evalCount.asInt() : 0;
        
        TokenUsage tokenUsage = new TokenUsage(
            promptTokens,
            completionTokens,
            promptTokens + completionTokens
        );
        lastTokenUsage.set(tokenUsage);
        
        String model = responseJson.path("model").asText();
        boolean done = responseJson.path("done").asBoolean(false);
        
        return ChatResponse.builder()
            .content(content)
            .tokenUsage(tokenUsage)
            .model(model)
            .finishReason(done ? "stop" : null)
            .success(true)
            .build();
    }

    @Override
    protected String extractStreamContent(String data) {
        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            return jsonNode.path("message").path("content").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
