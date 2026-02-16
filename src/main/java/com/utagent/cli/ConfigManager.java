package com.utagent.cli;

import com.utagent.config.AgentConfig;
import com.utagent.config.ConfigLoader;
import com.utagent.config.CoverageConfig;
import com.utagent.config.OutputConfig;
import com.utagent.llm.LLMConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Configuration manager for CLI tool. Handles loading and merging configuration.
 */
public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private final File source;
    private final File configFile;
    private final Double targetCoverage;
    private final Integer maxIterations;
    private final String apiKey;
    private final String apiUrl;
    private final String provider;
    private final String model;
    private final File outputDir;
    private final Boolean verbose;

    public ConfigManager(
            File source,
            File configFile,
            Double targetCoverage,
            Integer maxIterations,
            String apiKey,
            String apiUrl,
            String provider,
            String model,
            File outputDir,
            Boolean verbose) {
        this.source = source;
        this.configFile = configFile;
        this.targetCoverage = targetCoverage;
        this.maxIterations = maxIterations;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.provider = provider;
        this.model = model;
        this.outputDir = outputDir;
        this.verbose = verbose;
    }

    /**
     * Load configuration from file or default location.
     */
    public AgentConfig loadConfiguration() {
        File projectRoot = getProjectRoot();
        logger.debug("Loading configuration from project root: {}", projectRoot.getAbsolutePath());
        
        AgentConfig baseConfig;
        if (configFile != null && configFile.exists()) {
            logger.info("Using configuration file: {}", configFile.getAbsolutePath());
            baseConfig = ConfigLoader.loadFromFile(configFile);
        } else {
            logger.debug("Using default configuration location");
            baseConfig = ConfigLoader.load(projectRoot);
        }
        
        AgentConfig mergedConfig = applyCliOverrides(baseConfig);
        logger.debug("Configuration loaded successfully");
        return mergedConfig;
    }

    /**
     * Apply CLI parameter overrides to the base configuration.
     */
    private AgentConfig applyCliOverrides(AgentConfig baseConfig) {
        logger.debug("Applying CLI overrides to configuration");
        
        AgentConfig.Builder builder = AgentConfig.builder();
        
        LLMConfig.Builder llmBuilder = LLMConfig.builder()
            .provider(provider != null ? provider : baseConfig.getLlm().provider())
            .apiKey(apiKey != null ? apiKey : baseConfig.getLlm().apiKey())
            .baseUrl(apiUrl != null ? apiUrl : baseConfig.getLlm().baseUrl())
            .model(model != null ? model : baseConfig.getLlm().model())
            .temperature(baseConfig.getLlm().temperature())
            .maxTokens(baseConfig.getLlm().maxTokens())
            .maxRetries(baseConfig.getLlm().maxRetries());
        builder.llm(llmBuilder.build());
        
        CoverageConfig.Builder coverageBuilder = CoverageConfig.builder()
            .target(targetCoverage != null ? targetCoverage : baseConfig.getCoverage().target())
            .maxIterations(maxIterations != null ? maxIterations : baseConfig.getCoverage().maxIterations())
            .includeBranchCoverage(baseConfig.getCoverage().includeBranchCoverage());
        builder.coverage(coverageBuilder.build());
        
        OutputConfig.Builder outputBuilder = OutputConfig.builder()
            .directory(outputDir != null ? outputDir.getPath() : baseConfig.getOutput().directory())
            .format(baseConfig.getOutput().format())
            .verbose(verbose != null ? verbose : baseConfig.getOutput().verbose())
            .colorOutput(baseConfig.getOutput().colorOutput())
            .showProgress(baseConfig.getOutput().getShowProgressOrDefault());
        builder.output(outputBuilder.build());
        
        builder.generation(baseConfig.getGeneration());
        
        return builder.build();
    }

    /**
     * Initialize default configuration file.
     */
    public int initializeConfig() {
        File projectRoot = getProjectRoot();
        logger.info("Initializing default configuration in: {}", projectRoot.getAbsolutePath());

        try {
            ConfigLoader.createDefaultConfig(projectRoot);
            logger.info("Default configuration created successfully");
            return 0;
        } catch (IOException e) {
            logger.error("Failed to create default configuration: {}", e.getMessage(), e);
            return 1;
        }
    }

    /**
     * Get project root directory.
     */
    public File getProjectRoot() {
        if (source == null) {
            logger.debug("Source is null, using current directory as project root");
            return new File(".");
        }
        
        if (source.isFile()) {
            File parentFile = source.getParentFile();
            if (parentFile != null) {
                logger.debug("Source is file, using parent directory as project root: {}", parentFile.getAbsolutePath());
                return parentFile;
            } else {
                logger.debug("Source is file but parent directory is null, using current directory as project root");
                return new File(".");
            }
        }
        
        File current = source;
        while (current != null) {
            if (new File(current, "pom.xml").exists() || 
                new File(current, "build.gradle").exists() ||
                new File(current, "build.gradle.kts").exists()) {
                logger.debug("Found project root with build file: {}", current.getAbsolutePath());
                return current;
            }
            current = current.getParentFile();
        }
        
        logger.debug("No build file found, using source directory as project root: {}", source.getAbsolutePath());
        return source;
    }

    /**
     * Get configuration file path for initialization message.
     */
    public String getConfigFilePath() {
        File projectRoot = getProjectRoot();
        String configPath = new File(projectRoot, ".java-ut-agent.yaml").getAbsolutePath();
        logger.debug("Configuration file path: {}", configPath);
        return configPath;
    }
}
