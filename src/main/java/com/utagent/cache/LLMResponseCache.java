package com.utagent.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.utagent.config.CacheConfig;
import com.utagent.llm.ChatRequest;
import com.utagent.llm.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * LLM 响应缓存，用于缓存 LLM API 的响应结果。
 * 基于请求内容的哈希值进行缓存，避免重复调用 API。
 */
public class LLMResponseCache {

    private static final Logger logger = LoggerFactory.getLogger(LLMResponseCache.class);
    private static final String CACHE_FILE_EXTENSION = ".llm.json";

    private final CacheConfig config;
    private final ObjectMapper objectMapper;
    private final File cacheDir;

    public LLMResponseCache() {
        this(CacheConfig.defaults());
    }

    public LLMResponseCache(CacheConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module());
        this.cacheDir = new File(config.getCacheDirectoryOrDefault(), "llm");
        initializeCache();
    }

    private void initializeCache() {
        if (!config.getEnabledOrDefault()) {
            return;
        }

        if (!cacheDir.exists()) {
            try {
                Files.createDirectories(cacheDir.toPath());
                logger.info("Created LLM cache directory: {}", cacheDir.getAbsolutePath());
            } catch (IOException e) {
                logger.warn("Failed to create LLM cache directory: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取缓存的响应
     *
     * @param request 请求对象
     * @return Optional containing ChatResponse if cache hit, empty otherwise
     */
    public Optional<ChatResponse> get(ChatRequest request) {
        if (!config.getEnabledOrDefault()) {
            return Optional.empty();
        }

        String requestHash = computeRequestHash(request);
        File cacheFile = getCacheFile(requestHash);

        if (!cacheFile.exists()) {
            return Optional.empty();
        }

        if (isExpired(cacheFile)) {
            logger.debug("LLM cache expired for request hash: {}", requestHash);
            if (!cacheFile.delete()) {
                logger.warn("Failed to delete expired cache file: {}", cacheFile.getAbsolutePath());
            }
            return Optional.empty();
        }

        try {
            CachedLLMResponse cachedResponse = objectMapper.readValue(cacheFile, CachedLLMResponse.class);
            logger.debug("LLM cache hit for request hash: {}", requestHash);
            return Optional.of(cachedResponse.response());
        } catch (IOException e) {
            logger.warn("Failed to read LLM cache for request hash {}: {}", requestHash, e.getMessage());
            if (!cacheFile.delete()) {
                logger.warn("Failed to delete corrupted cache file: {}", cacheFile.getAbsolutePath());
            }
            return Optional.empty();
        }
    }

    /**
     * 存储响应到缓存
     *
     * @param request  请求对象
     * @param response 响应对象
     */
    public void put(ChatRequest request, ChatResponse response) {
        if (!config.getEnabledOrDefault()) {
            return;
        }

        if (!response.isSuccess()) {
            logger.debug("Not caching failed response");
            return;
        }

        try {
            String requestHash = computeRequestHash(request);
            File cacheFile = getCacheFile(requestHash);
            Files.createDirectories(cacheFile.getParentFile().toPath());

            CachedLLMResponse cachedResponse = new CachedLLMResponse(
                response,
                requestHash,
                Instant.now().toEpochMilli()
            );

            objectMapper.writeValue(cacheFile, cachedResponse);
            logger.debug("Cached LLM response for request hash: {}", requestHash);

            cleanupIfNeeded();
        } catch (IOException e) {
            logger.warn("Failed to cache LLM response: {}", e.getMessage());
        }
    }

    /**
     * 清除所有缓存
     */
    public void clear() {
        if (cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteRecursive(file);
                }
            }
            logger.info("LLM cache cleared");
        }
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStatistics getStatistics() {
        long entryCount = 0;
        long totalSize = 0;

        if (cacheDir.exists()) {
            entryCount = countCacheFiles(cacheDir);
            totalSize = calculateCacheSize(cacheDir);
        }

        return new CacheStatistics(entryCount, totalSize, cacheDir.getAbsolutePath());
    }

    private String computeRequestHash(ChatRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.model() != null ? request.model() : "default");
        sb.append(request.temperature());
        sb.append(request.maxTokens());

        for (var message : request.messages()) {
            sb.append(message.role());
            sb.append(message.content());
        }

        return computeHash(sb.toString());
    }

    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(content.hashCode());
        }
    }

    private File getCacheFile(String hash) {
        String subDir = hash.substring(0, 2);
        return new File(cacheDir, subDir + File.separator + hash + CACHE_FILE_EXTENSION);
    }

    private boolean isExpired(File cacheFile) {
        long maxAgeMillis = config.getMaxAgeMinutesOrDefault() * 60 * 1000;
        long cacheAge = System.currentTimeMillis() - cacheFile.lastModified();
        return cacheAge > maxAgeMillis;
    }

    private long countCacheFiles(File dir) {
        long count = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countCacheFiles(file);
                } else if (file.getName().endsWith(CACHE_FILE_EXTENSION)) {
                    count++;
                }
            }
        }
        return count;
    }

    private long calculateCacheSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += calculateCacheSize(file);
                } else if (file.getName().endsWith(CACHE_FILE_EXTENSION)) {
                    size += file.length();
                }
            }
        }
        return size;
    }

    private void cleanupIfNeeded() {
        long maxSizeBytes = config.getMaxSizeMBOrDefault() * 1024 * 1024;
        long currentSize = getStatistics().totalSizeBytes();

        if (currentSize > maxSizeBytes) {
            logger.info("LLM cache size ({} bytes) exceeds limit ({} bytes), cleaning up", currentSize, maxSizeBytes);
            cleanupOldEntries();
        }
    }

    private void cleanupOldEntries() {
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(CACHE_FILE_EXTENSION));
        if (files == null || files.length == 0) {
            return;
        }

        java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

        long targetSize = config.getMaxSizeMBOrDefault() * 1024 * 1024 / 2;
        long currentSize = getStatistics().totalSizeBytes();

        for (File file : files) {
            if (currentSize <= targetSize) {
                break;
            }
            currentSize -= file.length();
            if (!file.delete()) {
                logger.warn("Failed to delete old cache file: {}", file.getAbsolutePath());
            }
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        if (!file.delete()) {
            logger.warn("Failed to delete file: {}", file.getAbsolutePath());
        }
    }

    /**
     * 缓存的 LLM 响应记录
     */
    public record CachedLLMResponse(
        ChatResponse response,
        String requestHash,
        long cachedAt
    ) {}

    /**
     * 缓存统计信息
     */
    public record CacheStatistics(
        long entryCount,
        long totalSizeBytes,
        String cacheDirectory
    ) {
        public double totalSizeMB() {
            return totalSizeBytes / (1024.0 * 1024.0);
        }
    }
}
