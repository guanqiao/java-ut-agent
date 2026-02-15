package com.utagent.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 指标管理器，用于收集和暴露应用程序指标。
 * 集成 Micrometer 支持 Prometheus 格式。
 */
public class MetricsManager {

    private static final Logger logger = LoggerFactory.getLogger(MetricsManager.class);
    private static volatile MetricsManager instance;

    private final MeterRegistry registry;
    private final boolean enabled;

    // 缓存指标
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong cacheSize = new AtomicLong(0);

    // 活跃任务数
    private final AtomicInteger activeGenerations = new AtomicInteger(0);
    private final AtomicInteger activeOptimizations = new AtomicInteger(0);

    // 计数器
    private final Counter filesProcessedCounter;
    private final Counter testsGeneratedCounter;
    private final Counter llmCallsCounter;
    private final Counter llmCacheHitsCounter;
    private final Counter errorsCounter;

    // 计时器
    private final Timer generationTimer;
    private final Timer optimizationTimer;
    private final Timer llmCallTimer;
    private final Timer parseTimer;

    private MetricsManager(boolean enabled) {
        this.enabled = enabled;
        this.registry = enabled ? new PrometheusMeterRegistry(PrometheusConfig.DEFAULT) : null;

        if (enabled) {
            // 注册缓存指标
            Gauge.builder("utagent.cache.hits", cacheHits, AtomicLong::get)
                .description("Number of cache hits")
                .register(registry);
            Gauge.builder("utagent.cache.misses", cacheMisses, AtomicLong::get)
                .description("Number of cache misses")
                .register(registry);
            Gauge.builder("utagent.cache.size", cacheSize, AtomicLong::get)
                .description("Current cache size")
                .register(registry);
            Gauge.builder("utagent.cache.hit.rate", this, MetricsManager::getCacheHitRate)
                .description("Cache hit rate")
                .register(registry);

            // 注册活跃任务指标
            Gauge.builder("utagent.generations.active", activeGenerations, AtomicInteger::get)
                .description("Number of active test generations")
                .register(registry);
            Gauge.builder("utagent.optimizations.active", activeOptimizations, AtomicInteger::get)
                .description("Number of active optimizations")
                .register(registry);

            // 注册计数器
            filesProcessedCounter = Counter.builder("utagent.files.processed")
                .description("Total number of files processed")
                .register(registry);
            testsGeneratedCounter = Counter.builder("utagent.tests.generated")
                .description("Total number of tests generated")
                .register(registry);
            llmCallsCounter = Counter.builder("utagent.llm.calls")
                .description("Total number of LLM API calls")
                .register(registry);
            llmCacheHitsCounter = Counter.builder("utagent.llm.cache.hits")
                .description("Total number of LLM cache hits")
                .register(registry);
            errorsCounter = Counter.builder("utagent.errors")
                .description("Total number of errors")
                .register(registry);

            // 注册计时器
            generationTimer = Timer.builder("utagent.generation.duration")
                .description("Test generation duration")
                .register(registry);
            optimizationTimer = Timer.builder("utagent.optimization.duration")
                .description("Optimization duration")
                .register(registry);
            llmCallTimer = Timer.builder("utagent.llm.call.duration")
                .description("LLM API call duration")
                .register(registry);
            parseTimer = Timer.builder("utagent.parse.duration")
                .description("Code parsing duration")
                .register(registry);

            logger.info("MetricsManager initialized with Prometheus registry");
        } else {
            filesProcessedCounter = null;
            testsGeneratedCounter = null;
            llmCallsCounter = null;
            llmCacheHitsCounter = null;
            errorsCounter = null;
            generationTimer = null;
            optimizationTimer = null;
            llmCallTimer = null;
            parseTimer = null;
        }
    }

    /**
     * 获取单例实例
     */
    public static MetricsManager getInstance() {
        return getInstance(true);
    }

    /**
     * 获取单例实例（指定是否启用）
     */
    public static MetricsManager getInstance(boolean enabled) {
        if (instance == null) {
            synchronized (MetricsManager.class) {
                if (instance == null) {
                    instance = new MetricsManager(enabled);
                }
            }
        }
        return instance;
    }

    /**
     * 重置实例（主要用于测试）
     */
    public static void resetInstance() {
        instance = null;
    }

    // ==================== 缓存指标 ====================

    public void recordCacheHit() {
        if (enabled) {
            cacheHits.incrementAndGet();
        }
    }

    public void recordCacheMiss() {
        if (enabled) {
            cacheMisses.incrementAndGet();
        }
    }

    public void updateCacheSize(long size) {
        if (enabled) {
            cacheSize.set(size);
        }
    }

    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    // ==================== 任务指标 ====================

    public void incrementActiveGenerations() {
        if (enabled) {
            activeGenerations.incrementAndGet();
        }
    }

    public void decrementActiveGenerations() {
        if (enabled) {
            activeGenerations.decrementAndGet();
        }
    }

    public void incrementActiveOptimizations() {
        if (enabled) {
            activeOptimizations.incrementAndGet();
        }
    }

    public void decrementActiveOptimizations() {
        if (enabled) {
            activeOptimizations.decrementAndGet();
        }
    }

    // ==================== 计数器 ====================

    public void incrementFilesProcessed() {
        if (enabled) {
            filesProcessedCounter.increment();
        }
    }

    public void incrementTestsGenerated(int count) {
        if (enabled) {
            testsGeneratedCounter.increment(count);
        }
    }

    public void incrementLlmCalls() {
        if (enabled) {
            llmCallsCounter.increment();
        }
    }

    public void incrementLlmCacheHits() {
        if (enabled) {
            llmCacheHitsCounter.increment();
        }
    }

    public void incrementErrors() {
        if (enabled) {
            errorsCounter.increment();
        }
    }

    // ==================== 计时器 ====================

    public Timer.Sample startGenerationTimer() {
        return enabled ? Timer.start(registry) : null;
    }

    public void stopGenerationTimer(Timer.Sample sample) {
        if (enabled && sample != null) {
            sample.stop(generationTimer);
        }
    }

    public Timer.Sample startOptimizationTimer() {
        return enabled ? Timer.start(registry) : null;
    }

    public void stopOptimizationTimer(Timer.Sample sample) {
        if (enabled && sample != null) {
            sample.stop(optimizationTimer);
        }
    }

    public Timer.Sample startLlmCallTimer() {
        return enabled ? Timer.start(registry) : null;
    }

    public void stopLlmCallTimer(Timer.Sample sample) {
        if (enabled && sample != null) {
            sample.stop(llmCallTimer);
        }
    }

    public Timer.Sample startParseTimer() {
        return enabled ? Timer.start(registry) : null;
    }

    public void stopParseTimer(Timer.Sample sample) {
        if (enabled && sample != null) {
            sample.stop(parseTimer);
        }
    }

    // ==================== 便捷方法 ====================

    public void recordGenerationTime(Runnable operation) {
        if (!enabled) {
            operation.run();
            return;
        }
        Timer.Sample sample = startGenerationTimer();
        incrementActiveGenerations();
        try {
            operation.run();
        } catch (Exception e) {
            incrementErrors();
            throw e;
        } finally {
            decrementActiveGenerations();
            stopGenerationTimer(sample);
        }
    }

    public void recordOptimizationTime(Runnable operation) {
        if (!enabled) {
            operation.run();
            return;
        }
        Timer.Sample sample = startOptimizationTimer();
        incrementActiveOptimizations();
        try {
            operation.run();
        } catch (Exception e) {
            incrementErrors();
            throw e;
        } finally {
            decrementActiveOptimizations();
            stopOptimizationTimer(sample);
        }
    }

    // ==================== 导出 ====================

    /**
     * 获取 Prometheus 格式的指标
     */
    public String scrape() {
        if (!enabled) {
            return "# Metrics disabled";
        }
        return ((PrometheusMeterRegistry) registry).scrape();
    }

    /**
     * 获取 MeterRegistry
     */
    public MeterRegistry getRegistry() {
        return registry;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
