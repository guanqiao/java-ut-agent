package com.utagent.team;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TeamConfigSync Tests")
class TeamConfigSyncTest {

    @TempDir
    Path tempDir;

    private TeamConfigSync sync;

    @BeforeEach
    void setUp() {
        sync = new TeamConfigSync();
    }

    @Nested
    @DisplayName("Configuration Export")
    class ConfigurationExport {

        @Test
        @DisplayName("Should export team configuration")
        void shouldExportTeamConfiguration() throws IOException {
            TeamConfig config = TeamConfig.builder()
                .teamName("MyTeam")
                .targetCoverage(0.9)
                .namingConvention("should{MethodName}")
                .build();
            
            Path exportPath = tempDir.resolve("team-config.json");
            sync.exportConfig(config, exportPath);
            
            assertThat(Files.exists(exportPath)).isTrue();
            String content = Files.readString(exportPath);
            assertThat(content).contains("MyTeam");
            assertThat(content).contains("0.9");
        }

        @Test
        @DisplayName("Should include test patterns in export")
        void shouldIncludeTestPatternsInExport() throws IOException {
            TeamConfig config = TeamConfig.builder()
                .teamName("PatternTeam")
                .testPatterns(List.of("AAA", "Given-When-Then"))
                .build();
            
            Path exportPath = tempDir.resolve("pattern-config.json");
            sync.exportConfig(config, exportPath);
            
            String content = Files.readString(exportPath);
            assertThat(content).contains("AAA");
            assertThat(content).contains("Given-When-Then");
        }
    }

    @Nested
    @DisplayName("Configuration Import")
    class ConfigurationImport {

        @Test
        @DisplayName("Should import team configuration")
        void shouldImportTeamConfiguration() throws IOException {
            String json = """
                {
                    "teamName": "ImportedTeam",
                    "targetCoverage": 0.85,
                    "namingConvention": "test{MethodName}"
                }
                """;
            
            Path importPath = tempDir.resolve("import-config.json");
            Files.writeString(importPath, json);
            
            TeamConfig config = sync.importConfig(importPath);
            
            assertThat(config).isNotNull();
            assertThat(config.getTeamName()).isEqualTo("ImportedTeam");
            assertThat(config.getTargetCoverage()).isEqualTo(0.85);
        }

        @Test
        @DisplayName("Should handle invalid JSON gracefully")
        void shouldHandleInvalidJsonGracefully() throws IOException {
            Path invalidPath = tempDir.resolve("invalid.json");
            Files.writeString(invalidPath, "not valid json");
            
            TeamConfig config = sync.importConfig(invalidPath);
            
            assertThat(config).isNull();
        }
    }

    @Nested
    @DisplayName("Configuration Merge")
    class ConfigurationMerge {

        @Test
        @DisplayName("Should merge configurations")
        void shouldMergeConfigurations() {
            TeamConfig base = TeamConfig.builder()
                .teamName("BaseTeam")
                .targetCoverage(0.8)
                .build();
            
            TeamConfig override = TeamConfig.builder()
                .targetCoverage(0.9)
                .namingConvention("should{MethodName}")
                .build();
            
            TeamConfig merged = sync.mergeConfigs(base, override);
            
            assertThat(merged.getTeamName()).isEqualTo("BaseTeam");
            assertThat(merged.getTargetCoverage()).isEqualTo(0.9);
            assertThat(merged.getNamingConvention()).isEqualTo("should{MethodName}");
        }

        @Test
        @DisplayName("Should merge test patterns")
        void shouldMergeTestPatterns() {
            TeamConfig base = TeamConfig.builder()
                .testPatterns(List.of("AAA"))
                .build();
            
            TeamConfig override = TeamConfig.builder()
                .testPatterns(List.of("Given-When-Then"))
                .build();
            
            TeamConfig merged = sync.mergeConfigs(base, override);
            
            assertThat(merged.getTestPatterns()).contains("AAA", "Given-When-Then");
        }
    }

    @Nested
    @DisplayName("Configuration Validation")
    class ConfigurationValidation {

        @Test
        @DisplayName("Should validate valid configuration")
        void shouldValidateValidConfiguration() {
            TeamConfig config = TeamConfig.builder()
                .teamName("ValidTeam")
                .targetCoverage(0.8)
                .build();
            
            ValidationResult result = sync.validateConfig(config);
            
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should detect invalid coverage value")
        void shouldDetectInvalidCoverageValue() {
            TeamConfig config = TeamConfig.builder()
                .teamName("InvalidCoverage")
                .targetCoverage(1.5)
                .build();
            
            ValidationResult result = sync.validateConfig(config);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(e -> e.contains("targetCoverage"));
        }

        @Test
        @DisplayName("Should detect missing team name")
        void shouldDetectMissingTeamName() {
            TeamConfig config = TeamConfig.builder()
                .targetCoverage(0.8)
                .build();
            
            ValidationResult result = sync.validateConfig(config);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(e -> e.contains("teamName"));
        }
    }

    @Nested
    @DisplayName("Version Control Integration")
    class VersionControlIntegration {

        @Test
        @DisplayName("Should generate config diff")
        void shouldGenerateConfigDiff() {
            TeamConfig oldConfig = TeamConfig.builder()
                .teamName("Team")
                .targetCoverage(0.8)
                .build();
            
            TeamConfig newConfig = TeamConfig.builder()
                .teamName("Team")
                .targetCoverage(0.9)
                .build();
            
            List<ConfigChange> diff = sync.generateDiff(oldConfig, newConfig);
            
            assertThat(diff).isNotEmpty();
            assertThat(diff).anyMatch(c -> c.getField().equals("targetCoverage"));
        }

        @Test
        @DisplayName("Should track config version")
        void shouldTrackConfigVersion() {
            TeamConfig config = TeamConfig.builder()
                .teamName("VersionedTeam")
                .version("1.0.0")
                .build();
            
            assertThat(config.getVersion()).isEqualTo("1.0.0");
        }
    }

    @Nested
    @DisplayName("Team Sharing")
    class TeamSharing {

        @Test
        @DisplayName("Should generate shareable link")
        void shouldGenerateShareableLink() {
            TeamConfig config = TeamConfig.builder()
                .teamName("SharedTeam")
                .targetCoverage(0.85)
                .build();
            
            String shareableLink = sync.generateShareableLink(config);
            
            assertThat(shareableLink).isNotEmpty();
        }

        @Test
        @DisplayName("Should parse shareable link")
        void shouldParseShareableLink() {
            TeamConfig original = TeamConfig.builder()
                .teamName("LinkTeam")
                .targetCoverage(0.85)
                .build();
            
            String link = sync.generateShareableLink(original);
            TeamConfig parsed = sync.parseShareableLink(link);
            
            assertThat(parsed).isNotNull();
            assertThat(parsed.getTeamName()).isEqualTo("LinkTeam");
        }
    }
}
