package com.utagent.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.utagent.config.CacheConfig;
import com.utagent.model.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Cache for storing and retrieving parse results.
 */
public class ParseResultCache {

    private static final Logger logger = LoggerFactory.getLogger(ParseResultCache.class);
    private static final String CACHE_FILE_EXTENSION = ".json";
    private static final String METADATA_FILE = "cache-metadata.json";

    private final CacheConfig config;
    private final ObjectMapper objectMapper;
    private final File cacheDir;

    public ParseResultCache() {
        this(CacheConfig.defaults());
    }

    public ParseResultCache(CacheConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module());
        this.cacheDir = new File(config.getCacheDirectoryOrDefault());
        initializeCache();
    }

    private void initializeCache() {
        if (!config.getEnabledOrDefault()) {
            return;
        }

        if (!cacheDir.exists()) {
            try {
                Files.createDirectories(cacheDir.toPath());
                logger.info("Created cache directory: {}", cacheDir.getAbsolutePath());
            } catch (IOException e) {
                logger.warn("Failed to create cache directory: {}", e.getMessage());
            }
        }
    }

    /**
     * Get cached ClassInfo for a file if available and not expired.
     *
     * @param sourceFile the source file
     * @return Optional containing ClassInfo if cache hit, empty otherwise
     */
    public Optional<ClassInfo> get(File sourceFile) {
        if (!config.getEnabledOrDefault()) {
            return Optional.empty();
        }

        File cacheFile = getCacheFile(sourceFile);
        if (!cacheFile.exists()) {
            return Optional.empty();
        }

        if (isExpired(cacheFile, sourceFile)) {
            logger.debug("Cache expired for: {}", sourceFile.getName());
            cacheFile.delete();
            return Optional.empty();
        }

        try {
            CachedResult cachedResult = objectMapper.readValue(cacheFile, CachedResult.class);
            logger.debug("Cache hit for: {}", sourceFile.getName());
            return Optional.of(cachedResult.classInfo());
        } catch (IOException e) {
            logger.warn("Failed to read cache for {}: {}", sourceFile.getName(), e.getMessage());
            cacheFile.delete();
            return Optional.empty();
        }
    }

    /**
     * Store ClassInfo in cache.
     *
     * @param sourceFile the source file
     * @param classInfo the parse result to cache
     */
    public void put(File sourceFile, ClassInfo classInfo) {
        if (!config.getEnabledOrDefault()) {
            return;
        }

        try {
            File cacheFile = getCacheFile(sourceFile);
            Files.createDirectories(cacheFile.getParentFile().toPath());

            String contentHash = computeHash(sourceFile);
            CachedResult cachedResult = new CachedResult(
                classInfo,
                sourceFile.lastModified(),
                contentHash,
                Instant.now().toEpochMilli()
            );

            objectMapper.writeValue(cacheFile, cachedResult);
            logger.debug("Cached parse result for: {}", sourceFile.getName());

            cleanupIfNeeded();
        } catch (IOException e) {
            logger.warn("Failed to cache parse result for {}: {}", sourceFile.getName(), e.getMessage());
        }
    }

    /**
     * Invalidate cache for a specific file.
     *
     * @param sourceFile the source file
     */
    public void invalidate(File sourceFile) {
        File cacheFile = getCacheFile(sourceFile);
        if (cacheFile.exists()) {
            cacheFile.delete();
            logger.debug("Invalidated cache for: {}", sourceFile.getName());
        }
    }

    /**
     * Clear all cached results.
     */
    public void clear() {
        if (cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteRecursive(file);
                }
            }
            logger.info("Cache cleared");
        }
    }

    /**
     * Get cache statistics.
     *
     * @return CacheStatistics object
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

    private File getCacheFile(File sourceFile) {
        String hash = computeHash(sourceFile.getAbsolutePath());
        String subDir = hash.substring(0, 2);
        return new File(cacheDir, subDir + File.separator + hash + CACHE_FILE_EXTENSION);
    }

    private boolean isExpired(File cacheFile, File sourceFile) {
        long maxAgeMillis = config.getMaxAgeMinutesOrDefault() * 60 * 1000;
        long cacheAge = System.currentTimeMillis() - cacheFile.lastModified();

        if (cacheAge > maxAgeMillis) {
            return true;
        }

        try {
            CachedResult cachedResult = objectMapper.readValue(cacheFile, CachedResult.class);
            String currentHash = computeHash(sourceFile);
            return !currentHash.equals(cachedResult.contentHash()) ||
                   sourceFile.lastModified() != cachedResult.lastModified();
        } catch (IOException e) {
            return true;
        }
    }

    private String computeHash(File file) throws IOException {
        byte[] content = Files.readAllBytes(file.toPath());
        return computeHashBytes(content);
    }

    private String computeHash(String content) {
        return computeHashBytes(content.getBytes());
    }

    private String computeHashBytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(bytes.hashCode());
        }
    }

    private void cleanupIfNeeded() {
        long maxSizeBytes = config.getMaxSizeMBOrDefault() * 1024 * 1024;
        long currentSize = getStatistics().totalSizeBytes();

        if (currentSize > maxSizeBytes) {
            logger.info("Cache size ({} bytes) exceeds limit ({} bytes), cleaning up", currentSize, maxSizeBytes);
            cleanupOldEntries();
        }
    }

    private void cleanupOldEntries() {
        File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(CACHE_FILE_EXTENSION));
        if (files == null || files.length == 0) {
            return;
        }

        // Sort by last modified (oldest first)
        java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

        long targetSize = config.getMaxSizeMBOrDefault() * 1024 * 1024 / 2; // Reduce to 50%
        long currentSize = getStatistics().totalSizeBytes();

        for (File file : files) {
            if (currentSize <= targetSize) {
                break;
            }
            currentSize -= file.length();
            file.delete();
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
        file.delete();
    }

    /**
     * Record for cached parse results.
     */
    public record CachedResult(
        ClassInfo classInfo,
        long lastModified,
        String contentHash,
        long cachedAt
    ) {}

    /**
     * Cache statistics.
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
