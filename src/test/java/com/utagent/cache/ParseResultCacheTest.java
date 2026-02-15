package com.utagent.cache;

import com.utagent.config.CacheConfig;
import com.utagent.model.ClassInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParseResultCache Tests")
class ParseResultCacheTest {

    @TempDir
    Path tempDir;

    private ParseResultCache cache;
    private File cacheDir;

    @BeforeEach
    void setUp() {
        cacheDir = new File(tempDir.toFile(), "test-cache");
        CacheConfig config = CacheConfig.builder()
            .enabled(true)
            .cacheDirectory(cacheDir.getAbsolutePath())
            .maxAgeMinutes(60L)
            .build();
        cache = new ParseResultCache(config);
    }

    @AfterEach
    void tearDown() {
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    @DisplayName("Should return empty when cache is disabled")
    void shouldReturnEmptyWhenCacheDisabled() {
        // Given
        CacheConfig disabledConfig = CacheConfig.builder()
            .enabled(false)
            .build();
        ParseResultCache disabledCache = new ParseResultCache(disabledConfig);
        File sourceFile = new File(tempDir.toFile(), "Test.java");

        // When
        Optional<ClassInfo> result = disabledCache.get(sourceFile);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when file not in cache")
    void shouldReturnEmptyWhenFileNotInCache() {
        // Given
        File sourceFile = new File(tempDir.toFile(), "Test.java");

        // When
        Optional<ClassInfo> result = cache.get(sourceFile);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should store ClassInfo in cache")
    void shouldStoreClassInfo() throws IOException {
        // Given
        File sourceFile = createJavaFile("TestClass.java", "public class TestClass {}");
        ClassInfo classInfo = createClassInfo("TestClass");

        // When
        cache.put(sourceFile, classInfo);

        // Then - verify cache statistics show entry was stored
        ParseResultCache.CacheStatistics stats = cache.getStatistics();
        assertEquals(1, stats.entryCount(), "Cache should contain 1 entry");
        assertTrue(stats.totalSizeBytes() > 0, "Cache should have non-zero size");
    }

    @Test
    @DisplayName("Should invalidate cache entry")
    void shouldInvalidateCacheEntry() throws IOException {
        // Given
        File sourceFile = createJavaFile("TestClass.java", "public class TestClass {}");
        ClassInfo classInfo = createClassInfo("TestClass");
        cache.put(sourceFile, classInfo);

        // When
        cache.invalidate(sourceFile);
        Optional<ClassInfo> result = cache.get(sourceFile);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should clear all cache entries")
    void shouldClearAllCacheEntries() throws IOException {
        // Given
        File file1 = createJavaFile("Class1.java", "public class Class1 {}");
        File file2 = createJavaFile("Class2.java", "public class Class2 {}");
        cache.put(file1, createClassInfo("Class1"));
        cache.put(file2, createClassInfo("Class2"));

        // When
        cache.clear();

        // Then
        assertTrue(cache.get(file1).isEmpty());
        assertTrue(cache.get(file2).isEmpty());
        assertEquals(0, cache.getStatistics().entryCount());
    }

    @Test
    @DisplayName("Should return cache statistics")
    void shouldReturnCacheStatistics() throws IOException {
        // Given
        File sourceFile = createJavaFile("TestClass.java", "public class TestClass {}");
        cache.put(sourceFile, createClassInfo("TestClass"));

        // When
        ParseResultCache.CacheStatistics stats = cache.getStatistics();

        // Then
        assertEquals(1, stats.entryCount());
        assertTrue(stats.totalSizeBytes() > 0);
        assertTrue(stats.totalSizeMB() >= 0);
        assertNotNull(stats.cacheDirectory());
    }

    @Test
    @DisplayName("Should detect expired cache entries")
    void shouldDetectExpiredCacheEntries() throws IOException, InterruptedException {
        // Given
        CacheConfig shortLivedConfig = CacheConfig.builder()
            .enabled(true)
            .cacheDirectory(cacheDir.getAbsolutePath())
            .maxAgeMinutes(0L) // Immediate expiration
            .build();
        ParseResultCache shortLivedCache = new ParseResultCache(shortLivedConfig);

        File sourceFile = createJavaFile("TestClass.java", "public class TestClass {}");
        shortLivedCache.put(sourceFile, createClassInfo("TestClass"));

        Thread.sleep(100); // Wait a bit

        // When
        Optional<ClassInfo> result = shortLivedCache.get(sourceFile);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should detect modified files")
    void shouldDetectModifiedFiles() throws IOException, InterruptedException {
        // Given
        File sourceFile = createJavaFile("TestClass.java", "public class TestClass {}");
        cache.put(sourceFile, createClassInfo("TestClass"));

        Thread.sleep(100); // Wait to ensure different timestamp
        Files.writeString(sourceFile.toPath(), "public class TestClass { public void newMethod() {} }");

        // When
        Optional<ClassInfo> result = cache.get(sourceFile);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle non-existent cache directory gracefully")
    void shouldHandleNonExistentCacheDirectory() {
        // Given
        CacheConfig config = CacheConfig.builder()
            .enabled(true)
            .cacheDirectory("/invalid/path/that/cannot/be/created")
            .build();

        // When & Then - Should not throw
        assertDoesNotThrow(() -> new ParseResultCache(config));
    }

    @Test
    @DisplayName("Should handle corrupted cache files gracefully")
    void shouldHandleCorruptedCacheFiles() throws IOException {
        // Given
        File sourceFile = createJavaFile("TestClass.java", "public class TestClass {}");
        cache.put(sourceFile, createClassInfo("TestClass"));

        // Corrupt the cache file
        File cacheFile = getCacheFile(sourceFile);
        if (cacheFile != null) {
            Files.writeString(cacheFile.toPath(), "invalid json");
        }

        // When
        Optional<ClassInfo> result = cache.get(sourceFile);

        // Then
        assertTrue(result.isEmpty());
    }

    private File createJavaFile(String fileName, String content) throws IOException {
        File file = new File(tempDir.toFile(), fileName);
        Files.writeString(file.toPath(), content);
        return file;
    }

    private ClassInfo createClassInfo(String className) {
        return new ClassInfo(
            "com.example",
            className,
            "com.example." + className
        );
    }

    private File getCacheFile(File sourceFile) {
        // This is a simplified version - actual implementation uses hash
        File[] subDirs = cacheDir.listFiles(File::isDirectory);
        if (subDirs != null && subDirs.length > 0) {
            File[] files = subDirs[0].listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null && files.length > 0) {
                return files[0];
            }
        }
        return null;
    }
}
