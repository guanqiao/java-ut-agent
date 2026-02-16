package com.utagent.context;

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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProjectContextAnalyzer Tests")
class ProjectContextAnalyzerTest {

    @TempDir
    Path tempDir;

    private ProjectContextAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ProjectContextAnalyzer();
    }

    @Nested
    @DisplayName("Project Structure Analysis")
    class ProjectStructureAnalysis {

        @Test
        @DisplayName("Should analyze project structure")
        void shouldAnalyzeProjectStructure() throws IOException {
            createSampleProject();
            
            ProjectContext context = analyzer.analyze(tempDir.toFile());
            
            assertThat(context).isNotNull();
            assertThat(context.getProjectRoot()).isEqualTo(tempDir.toFile());
        }

        @Test
        @DisplayName("Should detect source directories")
        void shouldDetectSourceDirectories() throws IOException {
            createSampleProject();
            
            ProjectContext context = analyzer.analyze(tempDir.toFile());
            
            assertThat(context.getSourceDirectories()).isNotEmpty();
        }

        @Test
        @DisplayName("Should detect test directories")
        void shouldDetectTestDirectories() throws IOException {
            createSampleProject();
            
            ProjectContext context = analyzer.analyze(tempDir.toFile());
            
            assertThat(context.getTestDirectories()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Dependency Analysis")
    class DependencyAnalysis {

        @Test
        @DisplayName("Should detect project dependencies")
        void shouldDetectProjectDependencies() throws IOException {
            createMavenProject();
            
            ProjectContext context = analyzer.analyze(tempDir.toFile());
            
            assertThat(context.getDependencies()).isNotEmpty();
        }

        @Test
        @DisplayName("Should detect Spring Boot dependency")
        void shouldDetectSpringBootDependency() throws IOException {
            createSpringBootProject();
            
            ProjectContext context = analyzer.analyze(tempDir.toFile());
            
            assertThat(context.hasSpringBoot()).isTrue();
        }

        @Test
        @DisplayName("Should detect testing frameworks")
        void shouldDetectTestingFrameworks() throws IOException {
            createMavenProject();
            
            ProjectContext context = analyzer.analyze(tempDir.toFile());
            
            assertThat(context.getTestingFrameworks()).contains("JUnit 5");
        }
    }

    @Nested
    @DisplayName("Class Relationship Analysis")
    class ClassRelationshipAnalysis {

        @Test
        @DisplayName("Should build class dependency graph")
        void shouldBuildClassDependencyGraph() throws IOException {
            createSampleProject();
            
            ProjectContext context = analyzer.analyze(tempDir.toFile());
            
            assertThat(context.getDependencyGraph()).isNotNull();
        }

        @Test
        @DisplayName("Should identify related classes")
        void shouldIdentifyRelatedClasses() throws IOException {
            createSampleProject();
            
            ProjectContext context = analyzer.analyze(tempDir.toFile());
            Set<String> related = context.getRelatedClasses("Calculator");
            
            assertThat(related).isNotNull();
        }
    }

    @Nested
    @DisplayName("Test Pattern Detection")
    class TestPatternDetection {

        @Test
        @DisplayName("Should detect existing test patterns")
        void shouldDetectExistingTestPatterns() throws IOException {
            createSampleProject();
            
            ProjectContext context = analyzer.analyze(tempDir.toFile());
            
            assertThat(context.getTestPatterns()).isNotNull();
        }

        @Test
        @DisplayName("Should detect naming conventions")
        void shouldDetectNamingConventions() throws IOException {
            createSampleProject();
            
            ProjectContext context = analyzer.analyze(tempDir.toFile());
            
            assertThat(context.getNamingConvention()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Context Caching")
    class ContextCaching {

        @Test
        @DisplayName("Should cache analysis results")
        void shouldCacheAnalysisResults() throws IOException {
            createSampleProject();
            
            ProjectContext context1 = analyzer.analyze(tempDir.toFile());
            ProjectContext context2 = analyzer.analyze(tempDir.toFile());
            
            assertThat(context1).isEqualTo(context2);
        }

        @Test
        @DisplayName("Should invalidate cache on file change")
        void shouldInvalidateCacheOnFileChange() throws IOException {
            createSampleProject();
            
            ProjectContext context1 = analyzer.analyze(tempDir.toFile());
            
            Files.writeString(tempDir.resolve("src/main/java/com/example/NewClass.java"), 
                "package com.example; public class NewClass {}");
            
            analyzer.invalidateCache(tempDir.toFile());
            ProjectContext context2 = analyzer.analyze(tempDir.toFile());
            
            assertThat(context1).isNotSameAs(context2);
        }
    }

    private void createSampleProject() throws IOException {
        Path srcMain = tempDir.resolve("src/main/java/com/example");
        Path srcTest = tempDir.resolve("src/test/java/com/example");
        Files.createDirectories(srcMain);
        Files.createDirectories(srcTest);

        Files.writeString(srcMain.resolve("Calculator.java"), """
            package com.example;
            public class Calculator {
                public int add(int a, int b) { return a + b; }
            }
            """);

        Files.writeString(srcTest.resolve("CalculatorTest.java"), """
            package com.example;
            import org.junit.jupiter.api.Test;
            class CalculatorTest {
                @Test void testAdd() {}
            }
            """);
    }

    private void createMavenProject() throws IOException {
        createSampleProject();
        
        Files.writeString(tempDir.resolve("pom.xml"), """
            <project>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """);
    }

    private void createSpringBootProject() throws IOException {
        createMavenProject();
        
        Files.writeString(tempDir.resolve("pom.xml"), """
            <project>
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """);
    }
}
