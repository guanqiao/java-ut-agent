package com.utagent.plugin;

import com.utagent.model.ClassInfo;
import com.utagent.model.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PluginService Tests")
class PluginServiceTest {

    private PluginService pluginService;

    @BeforeEach
    void setUp() {
        pluginService = new PluginService();
    }

    @Nested
    @DisplayName("Context Extraction")
    class ContextExtraction {

        @Test
        @DisplayName("Should extract class from editor context")
        void shouldExtractClassFromEditorContext() {
            EditorContext context = new EditorContext(
                "Calculator.java",
                "src/main/java/com/example/Calculator.java",
                "com.example",
                "Calculator"
            );
            
            ClassInfo classInfo = pluginService.extractClassInfo(context);
            
            assertThat(classInfo).isNotNull();
            assertThat(classInfo.className()).isEqualTo("Calculator");
            assertThat(classInfo.packageName()).isEqualTo("com.example");
        }

        @Test
        @DisplayName("Should extract method at cursor position")
        void shouldExtractMethodAtCursorPosition() {
            EditorContext context = new EditorContext(
                "Calculator.java",
                "src/main/java/com/example/Calculator.java",
                "com.example",
                "Calculator"
            );
            context.setCursorLine(15);
            context.setCursorColumn(10);
            
            MethodInfo method = pluginService.extractMethodAtCursor(context);
            
            assertThat(method).isNotNull();
        }
    }

    @Nested
    @DisplayName("Test Generation Actions")
    class TestGenerationActions {

        @Test
        @DisplayName("Should generate test for current class")
        void shouldGenerateTestForCurrentClass() {
            EditorContext context = new EditorContext(
                "Calculator.java",
                "src/main/java/com/example/Calculator.java",
                "com.example",
                "Calculator"
            );
            
            GenerationResult result = pluginService.generateTestForClass(context);
            
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getGeneratedCode()).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate test for current method")
        void shouldGenerateTestForCurrentMethod() {
            EditorContext context = new EditorContext(
                "Calculator.java",
                "src/main/java/com/example/Calculator.java",
                "com.example",
                "Calculator"
            );
            context.setCursorLine(15);
            
            GenerationResult result = pluginService.generateTestForMethod(context);
            
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should handle generation failure gracefully")
        void shouldHandleGenerationFailureGracefully() {
            EditorContext context = new EditorContext(
                "Invalid.java",
                "non-existent-path",
                "",
                ""
            );
            
            GenerationResult result = pluginService.generateTestForClass(context);
            
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Coverage Display")
    class CoverageDisplay {

        @Test
        @DisplayName("Should calculate coverage for class")
        void shouldCalculateCoverageForClass() {
            EditorContext context = new EditorContext(
                "Calculator.java",
                "src/main/java/com/example/Calculator.java",
                "com.example",
                "Calculator"
            );
            
            CoverageInfo coverage = pluginService.calculateCoverage(context);
            
            assertThat(coverage).isNotNull();
            assertThat(coverage.getLineCoverage()).isGreaterThanOrEqualTo(0.0);
            assertThat(coverage.getLineCoverage()).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should get uncovered lines")
        void shouldGetUncoveredLines() {
            EditorContext context = new EditorContext(
                "Calculator.java",
                "src/main/java/com/example/Calculator.java",
                "com.example",
                "Calculator"
            );
            
            List<Integer> uncoveredLines = pluginService.getUncoveredLines(context);
            
            assertThat(uncoveredLines).isNotNull();
        }
    }

    @Nested
    @DisplayName("Diff Preview")
    class DiffPreview {

        @Test
        @DisplayName("Should generate diff for new test")
        void shouldGenerateDiffForNewTest() {
            String existingCode = "";
            String newCode = "class CalculatorTest { void testAdd() {} }";
            
            DiffResult diff = pluginService.generateDiff(existingCode, newCode);
            
            assertThat(diff).isNotNull();
            assertThat(diff.hasChanges()).isTrue();
        }

        @Test
        @DisplayName("Should generate diff for modified test")
        void shouldGenerateDiffForModifiedTest() {
            String existingCode = "class CalculatorTest { void testAdd() {} }";
            String newCode = "class CalculatorTest { void testAdd() {} void testSubtract() {} }";
            
            DiffResult diff = pluginService.generateDiff(existingCode, newCode);
            
            assertThat(diff).isNotNull();
            assertThat(diff.hasChanges()).isTrue();
            assertThat(diff.getAddedLines()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle no changes")
        void shouldHandleNoChanges() {
            String code = "class CalculatorTest { void testAdd() {} }";
            
            DiffResult diff = pluginService.generateDiff(code, code);
            
            assertThat(diff.hasChanges()).isFalse();
        }
    }

    @Nested
    @DisplayName("Quick Actions")
    class QuickActions {

        @Test
        @DisplayName("Should suggest quick fix for test failure")
        void shouldSuggestQuickFixForTestFailure() {
            TestFailureInfo failure = new TestFailureInfo(
                "CalculatorTest",
                "testAdd",
                "expected: <5> but was: <4>",
                10
            );
            
            QuickFixAction fix = pluginService.suggestQuickFix(failure);
            
            assertThat(fix).isNotNull();
            assertThat(fix.getDescription()).isNotEmpty();
        }

        @Test
        @DisplayName("Should provide navigation to test")
        void shouldProvideNavigationToTest() {
            EditorContext context = new EditorContext(
                "Calculator.java",
                "src/main/java/com/example/Calculator.java",
                "com.example",
                "Calculator"
            );
            
            NavigationTarget target = pluginService.getTestNavigationTarget(context);
            
            assertThat(target).isNotNull();
            assertThat(target.getFilePath()).contains("CalculatorTest.java");
        }
    }

    @Nested
    @DisplayName("Settings")
    class Settings {

        @Test
        @DisplayName("Should load plugin settings")
        void shouldLoadPluginSettings() {
            PluginSettings settings = pluginService.loadSettings();
            
            assertThat(settings).isNotNull();
            assertThat(settings.getTargetCoverage()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should save plugin settings")
        void shouldSavePluginSettings() {
            PluginSettings settings = new PluginSettings();
            settings.setTargetCoverage(0.9);
            settings.setIncludePrivateMethods(true);
            
            pluginService.saveSettings(settings);
            
            PluginSettings loaded = pluginService.loadSettings();
            assertThat(loaded.getTargetCoverage()).isEqualTo(0.9);
            assertThat(loaded.isIncludePrivateMethods()).isTrue();
        }
    }
}
