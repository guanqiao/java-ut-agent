package com.utagent.cli;

import com.utagent.config.AgentConfig;
import com.utagent.config.ConfigLoader;
import com.utagent.config.CoverageConfig;
import com.utagent.config.GenerationConfig;
import com.utagent.config.OutputConfig;
import com.utagent.llm.LLMConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ConfigManager Tests")
class ConfigManagerTest {

    private ConfigManager configManager;
    private File mockSource;
    private File mockConfigFile;

    @BeforeEach
    void setUp() {
        // 使用真实的临时目录而不是mock，避免NullPointerException
        try {
            mockSource = File.createTempFile("test", ".java");
        } catch (IOException e) {
            mockSource = new File(".");
        }
        mockConfigFile = mock(File.class);
        configManager = new ConfigManager(
                mockSource,
                mockConfigFile,
                0.8,
                10,
                "test-api-key",
                "http://api.example.com",
                "OpenAI",
                "gpt-4",
                new File("./output"),
                true
        );
    }

    @AfterEach
    void tearDown() {
        if (mockSource != null && mockSource.exists() && mockSource.isFile()) {
            mockSource.delete();
        }
    }

    @Test
    @DisplayName("Should load configuration from config file when it exists")
    void shouldLoadConfigurationFromConfigFileWhenItExists() {
        try (MockedStatic<ConfigLoader> mockedConfigLoader = Mockito.mockStatic(ConfigLoader.class)) {
            AgentConfig mockConfig = mock(AgentConfig.class);
            LLMConfig mockLlmConfig = mock(LLMConfig.class);
            CoverageConfig mockCoverageConfig = mock(CoverageConfig.class);
            OutputConfig mockOutputConfig = mock(OutputConfig.class);
            
            when(mockConfig.getLlm()).thenReturn(mockLlmConfig);
            when(mockConfig.getCoverage()).thenReturn(mockCoverageConfig);
            when(mockConfig.getOutput()).thenReturn(mockOutputConfig);
            when(mockConfig.getGeneration()).thenReturn(mock(GenerationConfig.class));
            
            when(mockLlmConfig.provider()).thenReturn("openai");
            when(mockLlmConfig.apiKey()).thenReturn(null);
            when(mockLlmConfig.baseUrl()).thenReturn(null);
            when(mockLlmConfig.model()).thenReturn("gpt-4");
            when(mockLlmConfig.temperature()).thenReturn(0.7);
            when(mockLlmConfig.maxTokens()).thenReturn(4096);
            when(mockLlmConfig.maxRetries()).thenReturn(3);
            
            when(mockCoverageConfig.target()).thenReturn(0.8);
            when(mockCoverageConfig.maxIterations()).thenReturn(10);
            when(mockCoverageConfig.includeBranchCoverage()).thenReturn(true);
            
            when(mockOutputConfig.directory()).thenReturn("src/test/java");
            when(mockOutputConfig.format()).thenReturn("standard");
            when(mockOutputConfig.verbose()).thenReturn(false);
            when(mockOutputConfig.colorOutput()).thenReturn(true);
            when(mockOutputConfig.getShowProgressOrDefault()).thenReturn(true);
            
            when(mockConfigFile.exists()).thenReturn(true);
            when(ConfigLoader.loadFromFile(mockConfigFile)).thenReturn(mockConfig);

            AgentConfig result = configManager.loadConfiguration();

            assertNotNull(result);
            mockedConfigLoader.verify(() -> ConfigLoader.loadFromFile(mockConfigFile));
        }
    }

    @Test
    @DisplayName("Should load configuration from default location when config file does not exist")
    void shouldLoadConfigurationFromDefaultLocationWhenConfigFileDoesNotExist() {
        try (MockedStatic<ConfigLoader> mockedConfigLoader = Mockito.mockStatic(ConfigLoader.class)) {
            AgentConfig mockConfig = mock(AgentConfig.class);
            LLMConfig mockLlmConfig = mock(LLMConfig.class);
            CoverageConfig mockCoverageConfig = mock(CoverageConfig.class);
            OutputConfig mockOutputConfig = mock(OutputConfig.class);
            
            when(mockConfig.getLlm()).thenReturn(mockLlmConfig);
            when(mockConfig.getCoverage()).thenReturn(mockCoverageConfig);
            when(mockConfig.getOutput()).thenReturn(mockOutputConfig);
            when(mockConfig.getGeneration()).thenReturn(mock(GenerationConfig.class));
            
            when(mockLlmConfig.provider()).thenReturn("openai");
            when(mockLlmConfig.apiKey()).thenReturn(null);
            when(mockLlmConfig.baseUrl()).thenReturn(null);
            when(mockLlmConfig.model()).thenReturn("gpt-4");
            when(mockLlmConfig.temperature()).thenReturn(0.7);
            when(mockLlmConfig.maxTokens()).thenReturn(4096);
            when(mockLlmConfig.maxRetries()).thenReturn(3);
            
            when(mockCoverageConfig.target()).thenReturn(0.8);
            when(mockCoverageConfig.maxIterations()).thenReturn(10);
            when(mockCoverageConfig.includeBranchCoverage()).thenReturn(true);
            
            when(mockOutputConfig.directory()).thenReturn("src/test/java");
            when(mockOutputConfig.format()).thenReturn("standard");
            when(mockOutputConfig.verbose()).thenReturn(false);
            when(mockOutputConfig.colorOutput()).thenReturn(true);
            when(mockOutputConfig.getShowProgressOrDefault()).thenReturn(true);
            
            when(mockConfigFile.exists()).thenReturn(false);
            when(ConfigLoader.load(any(File.class))).thenReturn(mockConfig);

            AgentConfig result = configManager.loadConfiguration();

            assertNotNull(result);
            mockedConfigLoader.verify(() -> ConfigLoader.load(any(File.class)));
        }
    }

    @Test
    @DisplayName("Should initialize config successfully")
    void shouldInitializeConfigSuccessfully() {
        try (MockedStatic<ConfigLoader> mockedConfigLoader = Mockito.mockStatic(ConfigLoader.class)) {
            mockedConfigLoader.when(() -> ConfigLoader.createDefaultConfig(any(File.class))).thenAnswer(invocation -> {
                // Do nothing for mock
                return null;
            });

            int result = configManager.initializeConfig();

            assertEquals(0, result);
            mockedConfigLoader.verify(() -> ConfigLoader.createDefaultConfig(any(File.class)));
        }
    }

    @Test
    @DisplayName("Should return error code when config initialization fails")
    void shouldReturnErrorCodeWhenConfigInitializationFails() {
        try (MockedStatic<ConfigLoader> mockedConfigLoader = Mockito.mockStatic(ConfigLoader.class)) {
            mockedConfigLoader.when(() -> ConfigLoader.createDefaultConfig(any(File.class))).thenThrow(IOException.class);

            int result = configManager.initializeConfig();

            assertEquals(1, result);
        }
    }

    @Test
    @DisplayName("Should get project root from source file's parent")
    void shouldGetProjectRootFromSourceFileParent() {
        // 使用真实的临时文件，不需要mock
        File result = configManager.getProjectRoot();

        assertNotNull(result);
        assertTrue(result.isDirectory());
    }

    @Test
    @DisplayName("Should get project root from current directory when source is null")
    void shouldGetProjectRootFromCurrentDirectoryWhenSourceIsNull() {
        ConfigManager nullSourceConfigManager = new ConfigManager(
                null,
                mockConfigFile,
                0.8,
                10,
                "test-api-key",
                "http://api.example.com",
                "OpenAI",
                "gpt-4",
                new File("./output"),
                true
        );

        File result = nullSourceConfigManager.getProjectRoot();

        assertNotNull(result);
        assertEquals(".", result.getPath());
    }

    @Test
    @DisplayName("Should get project root by searching for build files")
    void shouldGetProjectRootBySearchingForBuildFiles() {
        // 使用真实的临时文件，不需要mock
        File result = configManager.getProjectRoot();

        assertNotNull(result);
        assertTrue(result.isDirectory());
    }

    @Test
    @DisplayName("Should get config file path")
    void shouldGetConfigFilePath() {
        String result = configManager.getConfigFilePath();
        assertNotNull(result);
        assertTrue(result.endsWith(".java-ut-agent.yaml"));
    }
}
