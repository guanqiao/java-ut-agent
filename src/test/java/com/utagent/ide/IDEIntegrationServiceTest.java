package com.utagent.ide;

import com.utagent.model.ClassInfo;
import com.utagent.model.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IDEIntegrationService Tests")
class IDEIntegrationServiceTest {

    private IDEIntegrationService service;

    @BeforeEach
    void setUp() {
        service = new IDEIntegrationService();
    }

    @Nested
    @DisplayName("Test Generation Request")
    class TestGenerationRequest {

        @Test
        @DisplayName("Should create request for class")
        void shouldCreateRequestForClass() {
            ClassInfo classInfo = createTestClassInfo();
            
            IDERequest request = service.createRequest(classInfo);
            
            assertThat(request).isNotNull();
            assertThat(request.getTargetClass()).isEqualTo(classInfo);
        }

        @Test
        @DisplayName("Should create request with options")
        void shouldCreateRequestWithOptions() {
            ClassInfo classInfo = createTestClassInfo();
            IDEOptions options = IDEOptions.builder()
                .includePrivateMethods(true)
                .targetCoverage(0.9)
                .build();
            
            IDERequest request = service.createRequest(classInfo, options);
            
            assertThat(request.getOptions().isIncludePrivateMethods()).isTrue();
            assertThat(request.getOptions().getTargetCoverage()).isEqualTo(0.9);
        }

        @Test
        @DisplayName("Should create request for specific method")
        void shouldCreateRequestForSpecificMethod() {
            ClassInfo classInfo = createTestClassInfo();
            MethodInfo method = createTestMethodInfo();
            
            IDERequest request = service.createRequestForMethod(classInfo, method);
            
            assertThat(request.getTargetMethod()).isEqualTo(method);
        }
    }

    @Nested
    @DisplayName("Test Generation Response")
    class TestGenerationResponse {

        @Test
        @DisplayName("Should generate test code")
        void shouldGenerateTestCode() {
            IDERequest request = service.createRequest(createTestClassInfo());
            
            IDEResponse response = service.generateTest(request);
            
            assertThat(response).isNotNull();
            assertThat(response.getGeneratedCode()).isNotEmpty();
            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should include coverage estimate")
        void shouldIncludeCoverageEstimate() {
            IDERequest request = service.createRequest(createTestClassInfo());
            
            IDEResponse response = service.generateTest(request);
            
            assertThat(response.getCoverageEstimate()).isGreaterThanOrEqualTo(0.0);
            assertThat(response.getCoverageEstimate()).isLessThanOrEqualTo(1.0);
        }

        @Test
        @DisplayName("Should handle generation failure")
        void shouldHandleGenerationFailure() {
            IDERequest request = new IDERequest(null, null, null);
            
            IDEResponse response = service.generateTest(request);
            
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getErrorMessage()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Test Preview")
    class TestPreview {

        @Test
        @DisplayName("Should format test code for preview")
        void shouldFormatTestCodeForPreview() {
            String testCode = "class CalculatorTest { void testAdd() {} }";
            
            String preview = service.formatForPreview(testCode);
            
            assertThat(preview).contains("class CalculatorTest");
        }

        @Test
        @DisplayName("Should highlight differences")
        void shouldHighlightDifferences() {
            String oldCode = "assertEquals(5, result);";
            String newCode = "assertEquals(6, result);";
            
            String diff = service.highlightDiff(oldCode, newCode);
            
            assertThat(diff).contains("5");
            assertThat(diff).contains("6");
        }
    }

    @Nested
    @DisplayName("Quick Actions")
    class QuickActions {

        @Test
        @DisplayName("Should generate quick fix for assertion")
        void shouldGenerateQuickFixForAssertion() {
            String testCode = "assertEquals(5, calc.add(2, 2));";
            String failureMessage = "expected: <5> but was: <4>";
            
            QuickFix fix = service.suggestQuickFix(testCode, failureMessage);
            
            assertThat(fix).isNotNull();
            assertThat(fix.getDescription()).isNotEmpty();
            assertThat(fix.getFixedCode()).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate quick fix for missing mock")
        void shouldGenerateQuickFixForMissingMock() {
            String testCode = "service.process(data);";
            String failureMessage = "NullPointerException: service is null";
            
            QuickFix fix = service.suggestQuickFix(testCode, failureMessage);
            
            assertThat(fix).isNotNull();
            assertThat(fix.getFixedCode()).containsAnyOf("@Mock", "Mockito.mock");
        }
    }

    @Nested
    @DisplayName("Context Integration")
    class ContextIntegration {

        @Test
        @DisplayName("Should extract context from file path")
        void shouldExtractContextFromFilepath() {
            String filePath = "src/main/java/com/example/Calculator.java";
            
            IDEContext context = service.extractContext(filePath);
            
            assertThat(context).isNotNull();
            assertThat(context.getPackageName()).isEqualTo("com.example");
            assertThat(context.getClassName()).isEqualTo("Calculator");
        }

        @Test
        @DisplayName("Should resolve test file location")
        void shouldResolveTestFileLocation() {
            String sourcePath = "src/main/java/com/example/Calculator.java";
            
            String testPath = service.resolveTestLocation(sourcePath);
            
            assertThat(testPath).contains("src/test/java");
            assertThat(testPath).contains("CalculatorTest.java");
        }
    }

    private ClassInfo createTestClassInfo() {
        return new ClassInfo(
            "com.example",
            "Calculator",
            "com.example.Calculator",
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );
    }

    private MethodInfo createTestMethodInfo() {
        return new MethodInfo(
            "add",
            "int",
            new ArrayList<>(),
            new ArrayList<>(),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false
        );
    }
}
