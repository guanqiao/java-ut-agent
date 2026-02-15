package com.utagent.llm;

import com.utagent.cache.LLMResponseCache;
import com.utagent.metrics.MetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 带缓存功能的 LLMProvider 装饰器。
 * 在调用底层 Provider 之前先检查缓存，响应后写入缓存。
 */
public class CachedLLMProvider implements LLMProvider {

    private static final Logger logger = LoggerFactory.getLogger(CachedLLMProvider.class);

    private final LLMProvider delegate;
    private final LLMResponseCache cache;

    public CachedLLMProvider(LLMProvider delegate) {
        this(delegate, new LLMResponseCache());
    }

    public CachedLLMProvider(LLMProvider delegate, LLMResponseCache cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public String name() {
        return delegate.name() + "(Cached)";
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        MetricsManager metrics = MetricsManager.getInstance();

        // 先检查缓存
        var cachedResponse = cache.get(request);
        if (cachedResponse.isPresent()) {
            logger.debug("Cache hit for LLM request, returning cached response");
            metrics.incrementLlmCacheHits();
            metrics.recordCacheHit();
            return cachedResponse.get();
        }

        // 缓存未命中，调用底层 Provider
        logger.debug("Cache miss for LLM request, calling provider: {}", delegate.name());
        metrics.recordCacheMiss();

        var sample = metrics.startLlmCallTimer();
        ChatResponse response;
        try {
            response = delegate.chat(request);
        } finally {
            metrics.stopLlmCallTimer(sample);
        }

        metrics.incrementLlmCalls();

        // 缓存成功的响应
        if (response.isSuccess()) {
            cache.put(request, response);
        }

        return response;
    }

    @Override
    public void chatStream(ChatRequest request, Consumer<String> chunkConsumer, Consumer<ChatResponse> completeConsumer) {
        // 流式响应不支持缓存，直接委托
        delegate.chatStream(request, chunkConsumer, completeConsumer);
    }

    @Override
    public boolean supportsStreaming() {
        return delegate.supportsStreaming();
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public TokenUsage getLastTokenUsage() {
        return delegate.getLastTokenUsage();
    }

    @Override
    public ChatResponse chatWithRetry(ChatRequest request, int maxRetries) {
        MetricsManager metrics = MetricsManager.getInstance();

        // 先检查缓存
        var cachedResponse = cache.get(request);
        if (cachedResponse.isPresent()) {
            logger.debug("Cache hit for LLM request with retry, returning cached response");
            metrics.incrementLlmCacheHits();
            metrics.recordCacheHit();
            return cachedResponse.get();
        }

        // 缓存未命中，调用底层 Provider
        logger.debug("Cache miss for LLM request with retry, calling provider: {}", delegate.name());
        metrics.recordCacheMiss();

        var sample = metrics.startLlmCallTimer();
        ChatResponse response;
        try {
            response = delegate.chatWithRetry(request, maxRetries);
        } finally {
            metrics.stopLlmCallTimer(sample);
        }

        metrics.incrementLlmCalls();

        // 缓存成功的响应
        if (response.isSuccess()) {
            cache.put(request, response);
        }

        return response;
    }

    /**
     * 获取缓存统计信息
     */
    public LLMResponseCache.CacheStatistics getCacheStatistics() {
        return cache.getStatistics();
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cache.clear();
        logger.info("LLM response cache cleared");
    }

    /**
     * 获取底层 Provider
     */
    public LLMProvider getDelegate() {
        return delegate;
    }
}
