package com.utagent.llm.provider;

import com.utagent.llm.ChatRequest;
import com.utagent.llm.ChatResponse;
import com.utagent.llm.LLMProvider;
import com.utagent.llm.SslUtils;
import com.utagent.llm.TokenUsage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class AbstractLLMProvider implements LLMProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    protected final OkHttpClient httpClient;
    protected final ObjectMapper objectMapper;
    protected final String apiKey;
    protected final String baseUrl;
    protected final String defaultModel;
    protected final String caCertPath;
    
    protected final AtomicReference<TokenUsage> lastTokenUsage = new AtomicReference<>(TokenUsage.empty());

    protected AbstractLLMProvider(String apiKey, String baseUrl, String defaultModel) {
        this(apiKey, baseUrl, defaultModel, null);
    }

    protected AbstractLLMProvider(String apiKey, String baseUrl, String defaultModel, String caCertPath) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.caCertPath = caCertPath;
        this.httpClient = createHttpClient(caCertPath);
        this.objectMapper = new ObjectMapper();
    }
    
    protected OkHttpClient createHttpClient() {
        return createHttpClient(null);
    }

    protected OkHttpClient createHttpClient(String caCertPath) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS);
        
        return SslUtils.configureSsl(builder, caCertPath).build();
    }

    @Override
    public TokenUsage getLastTokenUsage() {
        return lastTokenUsage.get();
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    protected abstract String getChatEndpoint();
    
    protected abstract ObjectNode buildRequestBody(ChatRequest request);
    
    protected abstract ChatResponse parseResponse(String responseBody) throws IOException;
    
    protected abstract String extractStreamContent(String data);
    
    protected Request.Builder createRequestBuilder() {
        return new Request.Builder()
            .addHeader("Content-Type", "application/json");
    }
    
    protected Request.Builder addAuthHeaders(Request.Builder builder) {
        if (apiKey != null && !apiKey.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + apiKey);
        }
        return builder;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            ObjectNode requestBody = buildRequestBody(request);
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request.Builder requestBuilder = createRequestBuilder()
                .url(getChatEndpoint())
                .post(RequestBody.create(jsonBody, JSON));
            
            addAuthHeaders(requestBuilder);

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.error("LLM API request failed: {} - {}", response.code(), errorBody);
                    return ChatResponse.error("API request failed: " + response.code() + " - " + errorBody);
                }

                String responseBody = response.body().string();
                return parseResponse(responseBody);
            }
        } catch (IOException e) {
            logger.error("Error calling LLM API", e);
            return ChatResponse.error("Error calling LLM API: " + e.getMessage());
        }
    }

    @Override
    public void chatStream(ChatRequest request, Consumer<String> chunkConsumer, Consumer<ChatResponse> completeConsumer) {
        try {
            ChatRequest streamRequest = request.withStream(true);
            ObjectNode requestBody = buildRequestBody(streamRequest);
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request.Builder requestBuilder = createRequestBuilder()
                .url(getChatEndpoint())
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(jsonBody, JSON));
            
            addAuthHeaders(requestBuilder);

            StringBuilder fullContent = new StringBuilder();
            
            EventSource.Factory factory = EventSources.createFactory(httpClient);
            EventSourceListener listener = new EventSourceListener() {
                @Override
                public void onEvent(EventSource eventSource, String id, String type, String data) {
                    if ("[DONE]".equals(data) || data.isEmpty()) {
                        return;
                    }
                    
                    String content = extractStreamContent(data);
                    if (content != null && !content.isEmpty()) {
                        fullContent.append(content);
                        chunkConsumer.accept(content);
                    }
                }
                
                @Override
                public void onClosed(EventSource eventSource) {
                    ChatResponse response = ChatResponse.builder()
                        .content(fullContent.toString())
                        .success(true)
                        .tokenUsage(TokenUsage.empty())
                        .build();
                    completeConsumer.accept(response);
                }
                
                @Override
                public void onFailure(EventSource eventSource, Throwable t, Response response) {
                    String errorMsg = t != null ? t.getMessage() : "Unknown streaming error";
                    logger.error("Streaming error", t);
                    completeConsumer.accept(ChatResponse.error(errorMsg));
                }
            };
            
            factory.newEventSource(requestBuilder.build(), listener);
            
        } catch (Exception e) {
            logger.error("Error setting up streaming", e);
            completeConsumer.accept(ChatResponse.error("Streaming error: " + e.getMessage()));
        }
    }

    protected ArrayNode buildMessagesArray(ChatRequest request) {
        ArrayNode messagesArray = objectMapper.createArrayNode();
        for (var msg : request.messages()) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.role());
            msgNode.put("content", msg.content());
        }
        return messagesArray;
    }
}
