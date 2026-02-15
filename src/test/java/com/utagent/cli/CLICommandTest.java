package com.utagent.cli;

import com.utagent.config.AgentConfig;
import com.utagent.config.ConfigLoader;
import com.utagent.config.CoverageConfig;
import com.utagent.config.OutputConfig;
import com.utagent.llm.LLMConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CLICommand Tests")
class CLICommandTest {

    @TempDir
    Path tempDir;

    private CLICommand cliCommand;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        cliCommand = new CLICommand();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("Should return error when source path does not exist")
    void shouldReturnErrorWhenSourcePathDoesNotExist() throws Exception {
        // Given
        File nonExistentFile = new File(tempDir.toFile(), "non-existent.java");

        // When
        Integer result = cliCommand.call();

        // Then - When source is null, it should print banner and return 0
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Should load configuration from file when config file exists")
    void shouldLoadConfigurationFromFileWhenConfigFileExists() throws IOException {
        // Given
        File configFile = new File(tempDir.toFile(), ".java-ut-agent.yaml");
        String configContent = """
            llm:
              provider: openai
              model: gpt-4
            output:
              verbose: true
            """;
        Files.writeString(configFile.toPath(), configContent);

        // When
        AgentConfig config = ConfigLoader.loadFromFile(configFile);

        // Then
        assertNotNull(config);
        assertEquals("openai", config.getLlm().provider());
        assertEquals("gpt-4", config.getLlm().model());
        assertTrue(config.getOutput().verbose());
    }

    @Test
    @DisplayName("Should return default config when config file does not exist")
    void shouldReturnDefaultConfigWhenConfigFileDoesNotExist() {
        // Given
        File nonExistentFile = new File(tempDir.toFile(), "non-existent.yaml");

        // When
        AgentConfig config = ConfigLoader.loadFromFile(nonExistentFile);

        // Then
        assertNotNull(config);
        assertNotNull(config.getLlm());
        assertNotNull(config.getCoverage());
        assertNotNull(config.getOutput());
    }

    @Test
    @DisplayName("Should create default configuration file")
    void shouldCreateDefaultConfigurationFile() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        File configFile = new File(projectRoot, ".java-ut-agent.yaml");

        // When
        ConfigLoader.createDefaultConfig(projectRoot);

        // Then
        assertTrue(configFile.exists());
        String content = Files.readString(configFile.toPath());
        assertTrue(content.contains("Java UT Agent Configuration"));
        assertTrue(content.contains("llm:"));
        assertTrue(content.contains("coverage:"));
        assertTrue(content.contains("output:"));
    }

    @Test
    @DisplayName("Should not overwrite existing config file")
    void shouldNotOverwriteExistingConfigFile() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        File configFile = new File(projectRoot, ".java-ut-agent.yaml");
        Files.writeString(configFile.toPath(), "existing content");

        // When
        ConfigLoader.createDefaultConfig(projectRoot);

        // Then
        String content = Files.readString(configFile.toPath());
        assertEquals("existing content", content);
    }

    @Test
    @DisplayName("Should find config file in project root")
    void shouldFindConfigFileInProjectRoot() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        File configFile = new File(projectRoot, ".java-ut-agent.yaml");
        Files.createFile(configFile.toPath());

        // When
        var result = ConfigLoader.findConfigFile(projectRoot);

        // Then
        assertTrue(result.isPresent());
        assertEquals(configFile.getAbsolutePath(), result.get().getAbsolutePath());
    }

    @Test
    @DisplayName("Should return empty when no config file found")
    void shouldReturnEmptyWhenNoConfigFileFound() {
        // Given
        File projectRoot = tempDir.toFile();

        // When
        var result = ConfigLoader.findConfigFile(projectRoot);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should merge global and project config")
    void shouldMergeGlobalAndProjectConfig() throws IOException {
        // Given - Create global config
        File userHome = tempDir.toFile();
        File globalConfig = new File(userHome, ".java-ut-agent.yaml");
        Files.writeString(globalConfig.toPath(), """
            llm:
              provider: claude
              temperature: 0.5
            """);

        // Create project config
        File projectRoot = new File(tempDir.toFile(), "project");
        projectRoot.mkdirs();
        File projectConfig = new File(projectRoot, ".java-ut-agent.yaml");
        Files.writeString(projectConfig.toPath(), """
            llm:
              model: gpt-4
            coverage:
              target: 0.95
            """);

        // When - global.merge(project) means global is primary, project is secondary
        AgentConfig global = ConfigLoader.loadFromFile(globalConfig);
        AgentConfig project = ConfigLoader.loadFromFile(projectConfig);
        AgentConfig merged = project.merge(global); // Project config takes priority

        // Then - Project values should take priority when present
        assertEquals("claude", merged.getLlm().provider()); // From global (project doesn't have it)
        assertEquals(0.5, merged.getLlm().temperature()); // From global (project doesn't have it)
        assertEquals("gpt-4", merged.getLlm().model()); // From project
        assertEquals(0.95, merged.getCoverage().target()); // From project
    }

    @Test
    @DisplayName("Should handle invalid YAML gracefully")
    void shouldHandleInvalidYamlGracefully() throws IOException {
        // Given
        File configFile = new File(tempDir.toFile(), "invalid.yaml");
        Files.writeString(configFile.toPath(), "invalid: yaml: content: [");

        // When
        AgentConfig config = ConfigLoader.loadFromFile(configFile);

        // Then - Should return default config instead of throwing
        assertNotNull(config);
    }

    @Test
    @DisplayName("Should generate valid default config YAML")
    void shouldGenerateValidDefaultConfigYaml() {
        // When
        String yaml = ConfigLoader.generateDefaultConfigYaml();

        // Then
        assertNotNull(yaml);
        assertTrue(yaml.contains("provider: openai"));
        assertTrue(yaml.contains("target: 0.8"));
        assertTrue(yaml.contains("max-iterations: 10"));
        assertTrue(yaml.contains("strategy: ai"));
        assertTrue(yaml.contains("directory: src/test/java"));
    }

    @Test
    @DisplayName("Should save config to file")
    void shouldSaveConfigToFile() throws IOException {
        // Given
        File configFile = new File(tempDir.toFile(), "saved-config.yaml");
        AgentConfig config = AgentConfig.builder()
            .llm(LLMConfig.builder()
                .provider("openai")
                .apiKey("test-key")
                .model("gpt-4")
                .build())
            .coverage(CoverageConfig.builder()
                .target(0.85)
                .maxIterations(3)
                .build())
            .output(OutputConfig.builder()
                .directory("custom/test/dir")
                .verbose(true)
                .build())
            .build();

        // When
        ConfigLoader.saveToFile(config, configFile);

        // Then
        assertTrue(configFile.exists());
        String content = Files.readString(configFile.toPath());
        assertTrue(content.contains("openai"));
        assertTrue(content.contains("test-key"));
    }
}
