package com.utagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    
    private static final String[] CONFIG_FILE_NAMES = {
        ".java-ut-agent.yaml",
        ".java-ut-agent.yml",
        "java-ut-agent.yaml",
        "java-ut-agent.yml"
    };
    
    private static final String GLOBAL_CONFIG_DIR = System.getProperty("user.home");
    
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    
    private ConfigLoader() {
    }
    
    public static AgentConfig load(File projectRoot) {
        AgentConfig globalConfig = loadGlobalConfig();
        AgentConfig projectConfig = loadProjectConfig(projectRoot);
        
        return globalConfig.merge(projectConfig);
    }
    
    public static AgentConfig loadProjectConfig(File projectRoot) {
        if (projectRoot == null || !projectRoot.isDirectory()) {
            return new AgentConfig();
        }
        
        for (String configName : CONFIG_FILE_NAMES) {
            File configFile = new File(projectRoot, configName);
            if (configFile.exists()) {
                logger.info("Loading project config from: {}", configFile.getAbsolutePath());
                return loadFromFile(configFile);
            }
        }
        
        return new AgentConfig();
    }
    
    public static AgentConfig loadGlobalConfig() {
        for (String configName : CONFIG_FILE_NAMES) {
            File configFile = new File(GLOBAL_CONFIG_DIR, configName);
            if (configFile.exists()) {
                logger.info("Loading global config from: {}", configFile.getAbsolutePath());
                return loadFromFile(configFile);
            }
        }
        
        return new AgentConfig();
    }
    
    public static AgentConfig loadFromFile(File configFile) {
        if (configFile == null || !configFile.exists()) {
            return new AgentConfig();
        }
        
        try {
            return yamlMapper.readValue(configFile, AgentConfig.class);
        } catch (IOException e) {
            logger.warn("Failed to load config from {}: {}", configFile.getAbsolutePath(), e.getMessage());
            return new AgentConfig();
        }
    }
    
    public static Optional<File> findConfigFile(File projectRoot) {
        if (projectRoot == null || !projectRoot.isDirectory()) {
            return Optional.empty();
        }
        
        for (String configName : CONFIG_FILE_NAMES) {
            File configFile = new File(projectRoot, configName);
            if (configFile.exists()) {
                return Optional.of(configFile);
            }
        }
        
        return Optional.empty();
    }
    
    public static void saveToFile(AgentConfig config, File configFile) throws IOException {
        yamlMapper.writeValue(configFile, config);
        logger.info("Config saved to: {}", configFile.getAbsolutePath());
    }
    
    public static void createDefaultConfig(File projectRoot) throws IOException {
        File configFile = new File(projectRoot, ".java-ut-agent.yaml");
        
        if (configFile.exists()) {
            logger.warn("Config file already exists: {}", configFile.getAbsolutePath());
            return;
        }
        
        String defaultConfig = generateDefaultConfigYaml();
        Files.writeString(configFile.toPath(), defaultConfig);
        logger.info("Created default config file: {}", configFile.getAbsolutePath());
    }
    
    public static String generateDefaultConfigYaml() {
        return """
            # Java UT Agent Configuration
            # Version: 1.0.0
            
            # LLM Provider Configuration
            llm:
              # Provider: openai, claude, ollama, deepseek
              provider: openai
              # API key (can also be set via environment variable)
              # api-key: ${OPENAI_API_KEY}
              # Custom API URL (optional)
              # api-url: https://api.openai.com/v1
              # Model to use
              model: gpt-4
              # Temperature for generation (0.0-2.0)
              temperature: 0.7
              # Maximum tokens in response
              max-tokens: 4096
              # Maximum retry attempts
              max-retries: 3
              # CA certificate path for custom SSL (optional)
              # Use this when connecting to LLM APIs through a proxy with custom CA
              # ca-cert-path: /path/to/ca-cert.pem
            
            # Coverage Configuration
            coverage:
              # Target coverage rate (0.0-1.0)
              target: 0.8
              # Maximum iterations for optimization
              max-iterations: 10
              # Include branch coverage in calculations
              include-branch-coverage: true
            
            # Test Generation Configuration
            generation:
              # Strategy: ai, template
              strategy: ai
              # Include negative test cases
              include-negative-tests: true
              # Include edge case tests
              include-edge-cases: true
              # Include parameterized tests
              include-parameterized-tests: false
              # Test data strategy: simple, instancio, builder
              test-data-strategy: simple
              # Verify mock interactions
              verify-mocks: true
            
            # Output Configuration
            output:
              # Output directory for generated tests
              directory: src/test/java
              # Output format: standard, compact
              format: standard
              # Enable verbose output
              verbose: false
              # Enable colored output
              color-output: true
              # Show progress bar
              show-progress: true
            """;
    }
    
    public static AgentConfig mergeWithCli(AgentConfig config, AgentConfig cliOverrides) {
        return config.merge(cliOverrides);
    }
}
