package com.utagent.generator.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LLMClient {

    private static final Logger logger = LoggerFactory.getLogger(LLMClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public LLMClient(String apiKey) {
        this(apiKey, OPENAI_API_URL, DEFAULT_MODEL);
    }

    public LLMClient(String apiKey, String apiUrl, String model) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl != null ? apiUrl : OPENAI_API_URL;
        this.model = model != null ? model : DEFAULT_MODEL;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, 0.7);
    }

    public String chat(String systemPrompt, String userMessage, double temperature) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user", userMessage));
        return chat(messages, temperature);
    }

    public String chat(List<Message> messages, double temperature) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", 4096);

            ArrayNode messagesArray = requestBody.putArray("messages");
            for (Message msg : messages) {
                ObjectNode msgNode = messagesArray.addObject();
                msgNode.put("role", msg.role());
                msgNode.put("content", msg.content());
            }

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.error("LLM API request failed: {} - {}", response.code(), errorBody);
                    throw new RuntimeException("LLM API request failed: " + response.code());
                }

                String responseBody = response.body().string();
                JsonNode responseJson = objectMapper.readTree(responseBody);
                
                return responseJson.path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();
            }
        } catch (IOException e) {
            logger.error("Error calling LLM API", e);
            throw new RuntimeException("Error calling LLM API", e);
        }
    }

    public String chatWithRetry(String systemPrompt, String userMessage, int maxRetries) {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts < maxRetries) {
            try {
                return chat(systemPrompt, userMessage);
            } catch (Exception e) {
                lastException = e;
                attempts++;
                logger.warn("LLM API call attempt {} failed, retrying...", attempts);
                try {
                    Thread.sleep(1000 * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        throw new RuntimeException("LLM API call failed after " + maxRetries + " attempts", lastException);
    }

    public record Message(String role, String content) {}
}
