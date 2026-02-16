package com.utagent.patterns;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TestPatternLibrary Tests")
class TestPatternLibraryTest {

    private TestPatternLibrary library;

    @BeforeEach
    void setUp() {
        library = new TestPatternLibrary();
    }

    @Nested
    @DisplayName("Pattern Retrieval")
    class PatternRetrieval {

        @Test
        @DisplayName("Should find pattern by name")
        void shouldFindPatternByName() {
            Optional<TestPattern> pattern = library.findByName("Arrange-Act-Assert");
            
            assertThat(pattern).isPresent();
            assertThat(pattern.get().getName()).isEqualTo("Arrange-Act-Assert");
        }

        @Test
        @DisplayName("Should find patterns by category")
        void shouldFindPatternsByCategory() {
            List<TestPattern> patterns = library.findByCategory(PatternCategory.STRUCTURE);
            
            assertThat(patterns).isNotEmpty();
        }

        @Test
        @DisplayName("Should find patterns by tag")
        void shouldFindPatternsByTag() {
            List<TestPattern> patterns = library.findByTag("mocking");
            
            assertThat(patterns).isNotEmpty();
        }

        @Test
        @DisplayName("Should return empty for non-existent pattern")
        void shouldReturnEmptyForNonExistentPattern() {
            Optional<TestPattern> pattern = library.findByName("NonExistentPattern");
            
            assertThat(pattern).isEmpty();
        }
    }

    @Nested
    @DisplayName("Pattern Application")
    class PatternApplication {

        @Test
        @DisplayName("Should apply AAA pattern")
        void shouldApplyAAAPattern() {
            TestPattern pattern = library.findByName("Arrange-Act-Assert").orElseThrow();
            String code = pattern.generateTemplate("Calculator", "add");
            
            assertThat(code).contains("Arrange");
            assertThat(code).contains("Act");
            assertThat(code).contains("Assert");
        }

        @Test
        @DisplayName("Should apply Given-When-Then pattern")
        void shouldApplyGivenWhenThenPattern() {
            TestPattern pattern = library.findByName("Given-When-Then").orElseThrow();
            String code = pattern.generateTemplate("UserService", "createUser");
            
            assertThat(code).contains("Given");
            assertThat(code).contains("When");
            assertThat(code).contains("Then");
        }

        @Test
        @DisplayName("Should apply Mock pattern")
        void shouldApplyMockPattern() {
            TestPattern pattern = library.findByName("Mock-Dependency").orElseThrow();
            String code = pattern.generateTemplate("OrderService", "processOrder");
            
            assertThat(code).contains("@Mock");
        }
    }

    @Nested
    @DisplayName("Pattern Categories")
    class PatternCategories {

        @Test
        @DisplayName("Should list all categories")
        void shouldListAllCategories() {
            List<PatternCategory> categories = library.getAllCategories();
            
            assertThat(categories).contains(
                PatternCategory.STRUCTURE,
                PatternCategory.MOCKING,
                PatternCategory.ASSERTION,
                PatternCategory.DATA
            );
        }

        @Test
        @DisplayName("Should get patterns count by category")
        void shouldGetPatternsCountByCategory() {
            int structureCount = library.countByCategory(PatternCategory.STRUCTURE);
            
            assertThat(structureCount).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Custom Patterns")
    class CustomPatterns {

        @Test
        @DisplayName("Should add custom pattern")
        void shouldAddCustomPattern() {
            TestPattern customPattern = TestPattern.builder()
                .name("Custom-Test-Pattern")
                .category(PatternCategory.CUSTOM)
                .description("A custom test pattern")
                .template("// Custom pattern template")
                .build();
            
            library.addPattern(customPattern);
            
            assertThat(library.findByName("Custom-Test-Pattern")).isPresent();
        }

        @Test
        @DisplayName("Should remove custom pattern")
        void shouldRemoveCustomPattern() {
            TestPattern customPattern = TestPattern.builder()
                .name("Removable-Pattern")
                .category(PatternCategory.CUSTOM)
                .description("A removable pattern")
                .template("// Template")
                .build();
            
            library.addPattern(customPattern);
            library.removePattern("Removable-Pattern");
            
            assertThat(library.findByName("Removable-Pattern")).isEmpty();
        }

        @Test
        @DisplayName("Should update existing pattern")
        void shouldUpdateExistingPattern() {
            TestPattern pattern = TestPattern.builder()
                .name("Updatable-Pattern")
                .category(PatternCategory.CUSTOM)
                .description("Original description")
                .template("// Original")
                .build();
            
            library.addPattern(pattern);
            
            TestPattern updated = TestPattern.builder()
                .name("Updatable-Pattern")
                .category(PatternCategory.CUSTOM)
                .description("Updated description")
                .template("// Updated")
                .build();
            
            library.updatePattern(updated);
            
            TestPattern found = library.findByName("Updatable-Pattern").orElseThrow();
            assertThat(found.getDescription()).isEqualTo("Updated description");
        }
    }

    @Nested
    @DisplayName("Pattern Recommendation")
    class PatternRecommendation {

        @Test
        @DisplayName("Should recommend patterns for service class")
        void shouldRecommendPatternsForServiceClass() {
            List<TestPattern> recommendations = library.recommendForClassType("Service");
            
            assertThat(recommendations).isNotEmpty();
        }

        @Test
        @DisplayName("Should recommend patterns for controller class")
        void shouldRecommendPatternsForControllerClass() {
            List<TestPattern> recommendations = library.recommendForClassType("Controller");
            
            assertThat(recommendations).isNotEmpty();
        }

        @Test
        @DisplayName("Should recommend patterns for repository class")
        void shouldRecommendPatternsForRepositoryClass() {
            List<TestPattern> recommendations = library.recommendForClassType("Repository");
            
            assertThat(recommendations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Pattern Export/Import")
    class PatternExportImport {

        @Test
        @DisplayName("Should export patterns to JSON")
        void shouldExportPatternsToJson() {
            String json = library.exportToJson();
            
            assertThat(json).isNotEmpty();
            assertThat(json).contains("Arrange-Act-Assert");
        }

        @Test
        @DisplayName("Should import patterns from JSON")
        void shouldImportPatternsFromJson() {
            String json = "[{\"name\":\"Imported-Pattern\",\"category\":\"CUSTOM\",\"description\":\"Imported\",\"template\":\"// Template\"}]";
            
            library.importFromJson(json);
            
            assertThat(library.findByName("Imported-Pattern")).isPresent();
        }
    }
}
