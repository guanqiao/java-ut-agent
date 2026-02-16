package com.utagent.generator.strategy;

import com.utagent.model.AnnotationInfo;
import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;
import com.utagent.parser.FrameworkType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SpringMvcTestStrategy implements TestGenerationStrategy {

    @Override
    public String generateTestClass(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(generatePackageDeclaration(classInfo));
        sb.append("\n");
        sb.append(generateImports(classInfo));
        sb.append("\n");
        sb.append(generateClassDeclaration(classInfo));
        sb.append("\n");
        sb.append(generateFields(classInfo));
        sb.append("\n");
        sb.append(generateSetupMethod(classInfo));
        sb.append("\n");
        
        for (MethodInfo method : classInfo.methods()) {
            if (!method.isPrivate() && !method.isAbstract()) {
                sb.append(generateTestMethod(classInfo, method));
                sb.append("\n");
            }
        }
        
        sb.append("}\n");
        
        return sb.toString();
    }

    @Override
    public String generateTestMethod(ClassInfo classInfo, String methodName, List<String> uncoveredLines) {
        MethodInfo method = classInfo.methods().stream()
            .filter(m -> m.name().equals(methodName))
            .findFirst()
            .orElse(null);
        
        if (method == null) return "";
        
        return generateTestMethod(classInfo, method);
    }

    @Override
    public String generateAdditionalTests(ClassInfo classInfo, List<CoverageInfo> coverageInfo) {
        StringBuilder sb = new StringBuilder();
        
        for (CoverageInfo info : coverageInfo) {
            if (info.getLineCoverageRate() < 1.0) {
                sb.append(generateCoverageTest(classInfo, info));
            }
        }
        
        return sb.toString();
    }

    @Override
    public Set<FrameworkType> getSupportedFrameworks() {
        return Set.of(FrameworkType.SPRING_MVC);
    }

    @Override
    public String getTestAnnotation() {
        return "@Test";
    }

    @Override
    public String[] getRequiredImports() {
        return new String[] {
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.BeforeEach",
            "org.junit.jupiter.api.DisplayName",
            "org.mockito.InjectMocks",
            "org.mockito.Mock",
            "org.mockito.MockitoAnnotations",
            "static org.junit.jupiter.api.Assertions.*",
            "static org.mockito.Mockito.*"
        };
    }

    private String generatePackageDeclaration(ClassInfo classInfo) {
        String testPackage = classInfo.packageName().replaceFirst("\\.src\\.main\\.", ".src.test.");
        if (testPackage.equals(classInfo.packageName())) {
            testPackage = classInfo.packageName();
        }
        return "package " + testPackage + ";\n";
    }

    private String generateImports(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        for (String imp : getRequiredImports()) {
            sb.append("import ").append(imp).append(";\n");
        }
        
        sb.append("import ").append(classInfo.fullyQualifiedName()).append(";\n");
        
        for (FieldInfo field : classInfo.fields()) {
            if (field.isDependencyInjection()) {
                String type = field.type();
                if (!type.startsWith("java.") && !type.startsWith("javax.") && !type.startsWith("jakarta.")) {
                    sb.append("import ").append(type).append(";\n");
                }
            }
        }
        
        return sb.toString();
    }

    private String generateClassDeclaration(ClassInfo classInfo) {
        String testClassName = classInfo.className() + "Test";
        StringBuilder sb = new StringBuilder();
        
        sb.append("@DisplayName(\"").append(classInfo.className()).append(" Unit Tests\")\n");
        sb.append("class ").append(testClassName).append(" {\n");
        
        return sb.toString();
    }

    private String generateFields(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("    @InjectMocks\n");
        sb.append("    private ").append(classInfo.className()).append(" ")
          .append(toCamelCase(classInfo.className())).append(";\n\n");
        
        for (FieldInfo field : classInfo.fields()) {
            if (field.isDependencyInjection()) {
                sb.append("    @Mock\n");
                sb.append("    private ").append(field.type()).append(" ")
                  .append(toCamelCase(field.type())).append(";\n");
            }
        }
        
        return sb.toString();
    }

    private String generateSetupMethod(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("    @BeforeEach\n");
        sb.append("    void setUp() {\n");
        sb.append("        MockitoAnnotations.openMocks(this);\n");
        sb.append("    }\n");
        
        return sb.toString();
    }

    private String generateTestMethod(ClassInfo classInfo, MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        
        String displayName = generateDisplayName(method);
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"").append(displayName).append("\")\n");
        sb.append("    void ").append(generateTestMethodName(method)).append("() {\n");
        
        if (!method.parameters().isEmpty()) {
            sb.append("        // Given\n");
            for (var param : method.parameters()) {
                sb.append("        ").append(param.type()).append(" ")
                  .append(param.name()).append(" = ").append(generateMockValue(param.type())).append(";\n");
            }
            sb.append("\n");
        }
        
        sb.append("        // When\n");
        sb.append("        ");
        if (!method.returnType().equals("void")) {
            sb.append(method.returnType()).append(" result = ");
        }
        sb.append(toCamelCase(classInfo.className())).append(".")
          .append(method.name()).append("(");
        sb.append(method.parameters().stream()
            .map(p -> p.name())
            .collect(Collectors.joining(", ")));
        sb.append(");\n\n");
        
        sb.append("        // Then\n");
        if (!method.returnType().equals("void")) {
            sb.append("        assertNotNull(result);\n");
        }
        
        sb.append("    }\n");
        
        return sb.toString();
    }

    private String generateCoverageTest(ClassInfo classInfo, CoverageInfo info) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Test uncovered lines in ")
          .append(info.methodName()).append("\")\n");
        sb.append("    void test").append(capitalize(info.methodName()))
          .append("UncoveredLines() {\n");
        sb.append("        // Test uncovered lines at: ")
          .append(info.lineNumber()).append("\n");
        sb.append("        // Add specific test case to cover these lines\n");
        sb.append("    }\n");
        
        return sb.toString();
    }

    private String generateDisplayName(MethodInfo method) {
        return "Should " + method.name().replaceAll("([A-Z])", " $1").toLowerCase();
    }

    private String generateTestMethodName(MethodInfo method) {
        return "should" + capitalize(method.name()) + "Successfully";
    }

    private String generateMockParameter(String name, String type) {
        return type + " " + name + " = " + generateMockValue(type);
    }

    private String generateMockValue(String type) {
        return switch (type) {
            case "String" -> "\"test\"";
            case "int", "Integer" -> "1";
            case "long", "Long" -> "1L";
            case "boolean", "Boolean" -> "true";
            case "double", "Double" -> "1.0";
            case "float", "Float" -> "1.0f";
            default -> "mock(" + type + ".class)";
        };
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
