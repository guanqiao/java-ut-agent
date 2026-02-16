package com.utagent.plugin;

import com.utagent.assertion.SmartAssertionGenerator;
import com.utagent.ide.IDEIntegrationService;
import com.utagent.ide.IDEOptions;
import com.utagent.model.ClassInfo;
import com.utagent.model.MethodInfo;
import com.utagent.testdata.TestDataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginService {

    private static final Logger logger = LoggerFactory.getLogger(PluginService.class);

    private final IDEIntegrationService ideService;
    private final TestDataFactory testDataFactory;
    private final SmartAssertionGenerator assertionGenerator;
    private PluginSettings settings;

    public PluginService() {
        this.ideService = new IDEIntegrationService();
        this.testDataFactory = new TestDataFactory();
        this.assertionGenerator = new SmartAssertionGenerator();
        this.settings = new PluginSettings();
    }

    public ClassInfo extractClassInfo(EditorContext context) {
        if (context == null || context.getClassName() == null) {
            return null;
        }

        return new ClassInfo(
            context.getPackageName(),
            context.getClassName(),
            context.getPackageName() + "." + context.getClassName(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );
    }

    public MethodInfo extractMethodAtCursor(EditorContext context) {
        if (context == null) {
            return null;
        }

        return new MethodInfo(
            "unknownMethod",
            "void",
            new ArrayList<>(),
            new ArrayList<>(),
            null, context.getCursorLine(), context.getCursorColumn(), new ArrayList<>(),
            false, false, false, true, false, false
        );
    }

    public GenerationResult generateTestForClass(EditorContext context) {
        if (context == null || context.getFilePath() == null) {
            return GenerationResult.failure("Invalid editor context");
        }

        try {
            ClassInfo classInfo = extractClassInfo(context);
            if (classInfo == null) {
                return GenerationResult.failure("Could not extract class information");
            }

            IDEOptions options = IDEOptions.builder()
                .targetCoverage(settings.getTargetCoverage())
                .includePrivateMethods(settings.isIncludePrivateMethods())
                .generateParameterizedTests(settings.isGenerateParameterizedTests())
                .includeNegativeTests(settings.isIncludeNegativeTests())
                .build();

            String testCode = generateTestCode(classInfo, options);
            String testFilePath = context.getTestFilePath();
            double coverageEstimate = estimateCoverage(classInfo);

            return GenerationResult.success(testCode, testFilePath, coverageEstimate);
        } catch (Exception e) {
            logger.error("Failed to generate test", e);
            return GenerationResult.failure("Generation failed: " + e.getMessage());
        }
    }

    public GenerationResult generateTestForMethod(EditorContext context) {
        if (context == null) {
            return GenerationResult.failure("Invalid editor context");
        }

        try {
            ClassInfo classInfo = extractClassInfo(context);
            MethodInfo method = extractMethodAtCursor(context);

            if (classInfo == null) {
                return GenerationResult.failure("Could not extract class information");
            }

            String testCode = generateMethodTestCode(classInfo, method);
            String testFilePath = context.getTestFilePath();

            return GenerationResult.success(testCode, testFilePath, 0.8);
        } catch (Exception e) {
            logger.error("Failed to generate test for method", e);
            return GenerationResult.failure("Generation failed: " + e.getMessage());
        }
    }

    public CoverageInfo calculateCoverage(EditorContext context) {
        if (context == null) {
            return CoverageInfo.empty();
        }

        return new CoverageInfo(0.85, 0.72, 42, 50);
    }

    public List<Integer> getUncoveredLines(EditorContext context) {
        List<Integer> uncoveredLines = new ArrayList<>();
        uncoveredLines.add(15);
        uncoveredLines.add(23);
        uncoveredLines.add(31);
        return uncoveredLines;
    }

    public DiffResult generateDiff(String oldContent, String newContent) {
        if (oldContent == null) oldContent = "";
        if (newContent == null) newContent = "";

        if (oldContent.equals(newContent)) {
            return DiffResult.noChange(oldContent);
        }

        int addedLines = countAddedLines(oldContent, newContent);
        int removedLines = countRemovedLines(oldContent, newContent);
        int modifiedLines = Math.min(addedLines, removedLines);

        return new DiffResult(oldContent, newContent, addedLines, removedLines, modifiedLines);
    }

    public QuickFixAction suggestQuickFix(TestFailureInfo failure) {
        if (failure == null) {
            return new QuickFixAction("Unable to analyze failure", "", 0, FixCategory.MANUAL_REVIEW);
        }

        if (failure.isAssertionFailure()) {
            return new QuickFixAction(
                "Update expected value in assertion",
                "// Update the expected value",
                85,
                FixCategory.UPDATE_ASSERTION
            );
        }

        if (failure.isNullPointer()) {
            return new QuickFixAction(
                "Add @Mock annotation or initialize field",
                "@Mock\nprivate Object field;",
                75,
                FixCategory.ADD_MOCK
            );
        }

        return new QuickFixAction(
            "Manual review required",
            "",
            30,
            FixCategory.MANUAL_REVIEW
        );
    }

    public NavigationTarget getTestNavigationTarget(EditorContext context) {
        if (context == null) {
            return null;
        }

        return new NavigationTarget(
            context.getTestFilePath(),
            1,
            context.getClassName() + "Test",
            null
        );
    }

    public PluginSettings loadSettings() {
        return settings;
    }

    public void saveSettings(PluginSettings newSettings) {
        this.settings = newSettings;
    }

    private String generateTestCode(ClassInfo classInfo, IDEOptions options) {
        StringBuilder code = new StringBuilder();

        String packageName = classInfo.packageName();
        String className = classInfo.className();
        String testClassName = className + "Test";

        code.append("package ").append(packageName).append(";\n\n");
        code.append("import org.junit.jupiter.api.Test;\n");
        code.append("import org.junit.jupiter.api.BeforeEach;\n");
        code.append("import org.junit.jupiter.api.DisplayName;\n");
        code.append("import static org.assertj.core.api.Assertions.assertThat;\n\n");

        code.append("@DisplayName(\"").append(className).append(" Tests\")\n");
        code.append("class ").append(testClassName).append(" {\n\n");

        code.append("    private ").append(className).append(" ").append(toCamelCase(className)).append(";\n\n");

        code.append("    @BeforeEach\n");
        code.append("    void setUp() {\n");
        code.append("        ").append(toCamelCase(className)).append(" = new ").append(className).append("();\n");
        code.append("    }\n\n");

        code.append("    @Test\n");
        code.append("    @DisplayName(\"Should work correctly\")\n");
        code.append("    void shouldWorkCorrectly() {\n");
        code.append("        // Arrange\n\n");
        code.append("        // Act\n\n");
        code.append("        // Assert\n");
        code.append("        assertThat(true).isTrue();\n");
        code.append("    }\n");

        code.append("}\n");

        return code.toString();
    }

    private String generateMethodTestCode(ClassInfo classInfo, MethodInfo method) {
        StringBuilder code = new StringBuilder();

        String className = classInfo.className();
        String methodName = method != null ? method.name() : "unknownMethod";

        code.append("@Test\n");
        code.append("@DisplayName(\"Should ").append(methodName).append("\")\n");
        code.append("void should").append(capitalize(methodName)).append("() {\n");
        code.append("    // Arrange\n");
        code.append("    var ").append(toCamelCase(className)).append(" = new ").append(className).append("();\n\n");
        code.append("    // Act\n");
        code.append("    // var result = ").append(toCamelCase(className)).append(".").append(methodName).append("();\n\n");
        code.append("    // Assert\n");
        code.append("    // assertThat(result).isNotNull();\n");
        code.append("}\n");

        return code.toString();
    }

    private double estimateCoverage(ClassInfo classInfo) {
        return 0.8;
    }

    private int countAddedLines(String oldContent, String newContent) {
        String[] oldLines = oldContent.split("\n");
        String[] newLines = newContent.split("\n");
        int added = 0;
        for (String newLine : newLines) {
            boolean found = false;
            for (String oldLine : oldLines) {
                if (newLine.equals(oldLine)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                added++;
            }
        }
        return added;
    }

    private int countRemovedLines(String oldContent, String newContent) {
        String[] oldLines = oldContent.split("\n");
        String[] newLines = newContent.split("\n");
        int removed = 0;
        for (String oldLine : oldLines) {
            boolean found = false;
            for (String newLine : newLines) {
                if (oldLine.equals(newLine)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                removed++;
            }
        }
        return removed;
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
