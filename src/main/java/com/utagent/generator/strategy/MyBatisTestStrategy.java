package com.utagent.generator.strategy;

import com.utagent.model.AnnotationInfo;
import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.model.MethodInfo;
import com.utagent.parser.FrameworkType;

import java.util.List;
import java.util.Set;

public class MyBatisTestStrategy implements TestGenerationStrategy {

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
        
        for (MethodInfo method : classInfo.methods()) {
            if (!method.isPrivate() && !method.isAbstract()) {
                sb.append(generateMapperTestMethod(classInfo, method));
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
        
        return generateMapperTestMethod(classInfo, method);
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
        return Set.of(FrameworkType.MYBATIS);
    }

    @Override
    public String getTestAnnotation() {
        return "@Test";
    }

    @Override
    public String[] getRequiredImports() {
        return new String[] {
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.DisplayName",
            "org.mybatis.spring.boot.test.autoconfigure.MybatisTest",
            "org.springframework.beans.factory.annotation.Autowired",
            "org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase",
            "org.springframework.test.context.ActiveProfiles",
            "static org.junit.jupiter.api.Assertions.*"
        };
    }

    private String generatePackageDeclaration(ClassInfo classInfo) {
        return "package " + classInfo.packageName() + ";\n";
    }

    private String generateImports(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;\n");
        sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        sb.append("import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;\n");
        sb.append("import org.springframework.test.context.ActiveProfiles;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n");
        sb.append("import ").append(classInfo.fullyQualifiedName()).append(";\n");
        
        return sb.toString();
    }

    private String generateClassDeclaration(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("@MybatisTest\n");
        sb.append("@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)\n");
        sb.append("@ActiveProfiles(\"test\")\n");
        sb.append("@DisplayName(\"").append(classInfo.className()).append(" Mapper Tests\")\n");
        sb.append("class ").append(classInfo.className()).append("Test {\n");
        
        return sb.toString();
    }

    private String generateFields(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("    @Autowired\n");
        sb.append("    private ").append(classInfo.className()).append(" ")
          .append(toCamelCase(classInfo.className())).append(";\n");
        
        return sb.toString();
    }

    private String generateMapperTestMethod(ClassInfo classInfo, MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        
        String operationType = detectOperationType(method);
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should ").append(operationType)
          .append(" ").append(method.name()).append(" successfully\")\n");
        sb.append("    void should").append(capitalize(method.name())).append("Successfully() {\n");
        sb.append("        // Given\n");
        
        for (var param : method.parameters()) {
            sb.append("        ").append(param.type()).append(" ")
              .append(param.name()).append(" = ")
              .append(generateTestValue(param.type())).append(";\n");
        }
        
        sb.append("\n");
        sb.append("        // When\n");
        
        if (!method.returnType().equals("void")) {
            sb.append("        ").append(method.returnType()).append(" result = ");
        }
        
        sb.append(toCamelCase(classInfo.className())).append(".")
          .append(method.name()).append("(");
        sb.append(String.join(", ", 
            method.parameters().stream().map(p -> p.name()).toList()));
        sb.append(");\n\n");
        
        sb.append("        // Then\n");
        
        switch (operationType) {
            case "select" -> {
                sb.append("        assertNotNull(result);\n");
                if (!method.returnType().equals("void") && 
                    !method.returnType().contains("List") &&
                    !method.returnType().contains("Optional")) {
                    sb.append("        // Verify the result contains expected data\n");
                }
            }
            case "insert" -> {
                sb.append("        // Verify the record was inserted\n");
                sb.append("        // You may need to query and verify\n");
            }
            case "update" -> {
                sb.append("        // Verify the record was updated\n");
            }
            case "delete" -> {
                sb.append("        // Verify the record was deleted\n");
            }
            default -> {
                if (!method.returnType().equals("void")) {
                    sb.append("        assertNotNull(result);\n");
                }
            }
        }
        
        sb.append("    }\n");
        
        return sb.toString();
    }

    private String generateCoverageTest(ClassInfo classInfo, CoverageInfo info) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Test edge case for ")
          .append(info.methodName()).append("\")\n");
        sb.append("    void test").append(capitalize(info.methodName()))
          .append("EdgeCase() {\n");
        sb.append("        // TODO: Add edge case test for uncovered scenarios\n");
        sb.append("    }\n");
        
        return sb.toString();
    }

    private String detectOperationType(MethodInfo method) {
        String methodName = method.name().toLowerCase();
        
        if (methodName.startsWith("select") || methodName.startsWith("find") ||
            methodName.startsWith("get") || methodName.startsWith("query") ||
            methodName.startsWith("list") || methodName.startsWith("count")) {
            return "select";
        }
        if (methodName.startsWith("insert") || methodName.startsWith("add") ||
            methodName.startsWith("create") || methodName.startsWith("save")) {
            return "insert";
        }
        if (methodName.startsWith("update") || methodName.startsWith("modify") ||
            methodName.startsWith("edit")) {
            return "update";
        }
        if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "delete";
        }
        
        for (AnnotationInfo annotation : method.annotations()) {
            String annoName = annotation.name().toLowerCase();
            if (annoName.contains("select")) return "select";
            if (annoName.contains("insert")) return "insert";
            if (annoName.contains("update")) return "update";
            if (annoName.contains("delete")) return "delete";
        }
        
        return "execute";
    }

    private String generateTestValue(String type) {
        if (type == null) return "null";
        
        return switch (type) {
            case "String" -> "\"test_value\"";
            case "int", "Integer" -> "1";
            case "long", "Long" -> "1L";
            case "boolean", "Boolean" -> "true";
            case "double", "Double" -> "1.0";
            case "float", "Float" -> "1.0f";
            case "LocalDate" -> "LocalDate.now()";
            case "LocalDateTime" -> "LocalDateTime.now()";
            case "Date" -> "new Date()";
            default -> {
                if (type.endsWith("Id")) {
                    yield "1L";
                }
                yield "null";
            }
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
