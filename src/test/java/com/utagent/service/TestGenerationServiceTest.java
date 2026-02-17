package com.utagent.service;

import com.utagent.config.AgentConfig;
import com.utagent.config.CoverageConfig;
import com.utagent.config.OutputConfig;
import com.utagent.generator.TestGenerator;
import com.utagent.model.ClassInfo;
import com.utagent.parser.FrameworkDetector;
import com.utagent.parser.FrameworkType;
import com.utagent.parser.JavaCodeParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TestGenerationService Tests")
class TestGenerationServiceTest {

    private TestGenerationService service;
    private AgentConfig config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        config = AgentConfig.builder()
            .coverage(CoverageConfig.builder()
                .target(0.8)
                .maxIterations(10)
                .build())
            .output(OutputConfig.builder()
                .verbose(true)
                .build())
            .build();

        service = new TestGenerationService(config);
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should get target coverage from config")
        void shouldGetTargetCoverageFromConfig() {
            assertEquals(0.8, service.getTargetCoverage());
        }

        @Test
        @DisplayName("Should get max iterations from config")
        void shouldGetMaxIterationsFromConfig() {
            assertEquals(10, service.getMaxIterations());
        }

        @Test
        @DisplayName("Should check verbose mode")
        void shouldCheckVerboseMode() {
            assertTrue(service.isVerbose());
        }
    }

    @Nested
    @DisplayName("Service Creation Tests")
    class ServiceCreationTests {

        @Test
        @DisplayName("Should create service with config")
        void shouldCreateServiceWithConfig() {
            assertNotNull(service);
        }

        @Test
        @DisplayName("Should create service with dependencies")
        void shouldCreateServiceWithDependencies() {
            JavaCodeParser parser = new JavaCodeParser();
            TestGenerator generator = new TestGenerator();
            FrameworkDetector detector = new FrameworkDetector();

            TestGenerationService customService = new TestGenerationService(
                config, parser, generator, detector
            );

            assertNotNull(customService);
        }
    }

    @Nested
    @DisplayName("Parse Source Tests")
    class ParseSourceTests {

        @Test
        @DisplayName("Should return empty for non-existent file")
        void shouldReturnEmptyForNonExistentFile() {
            File nonExistent = new File("non-existent-file.java");

            Optional<ClassInfo> result = service.parseSource(nonExistent);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Framework Detection Tests")
    class FrameworkDetectionTests {

        @Test
        @DisplayName("Should detect frameworks for class")
        void shouldDetectFrameworksForClass() {
            ClassInfo classInfo = new ClassInfo(
                "com.example",
                "TestClass",
                "com.example.TestClass"
            );

            Set<FrameworkType> frameworks = service.detectFrameworks(classInfo);

            assertNotNull(frameworks);
        }
    }

    @Nested
    @DisplayName("Result Tests")
    class ResultTests {

        @Test
        @DisplayName("Should create successful result")
        void shouldCreateSuccessfulResult() {
            GenerationResult result = GenerationResult.success(
                "Generated test code",
                1,
                0.85
            );

            assertTrue(result.success());
            assertEquals("Generated test code", result.testCode());
            assertEquals(1, result.iterations());
            assertEquals(0.85, result.coverage());
            assertNull(result.errorMessage());
        }

        @Test
        @DisplayName("Should create failure result")
        void shouldCreateFailureResult() {
            GenerationResult result = GenerationResult.failure("Error occurred");

            assertFalse(result.success());
            assertNull(result.testCode());
            assertEquals("Error occurred", result.errorMessage());
        }
    }
}
