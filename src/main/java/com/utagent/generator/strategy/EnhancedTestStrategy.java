package com.utagent.generator.strategy;

import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;
import com.utagent.model.ParameterInfo;
import com.utagent.parser.FrameworkType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class EnhancedTestStrategy implements TestGenerationStrategy {

    protected boolean includeNegativeTests = true;
    protected boolean includeEdgeCases = true;
    protected boolean includeParameterizedTests = false;
    
    public void setIncludeNegativeTests(boolean include) {
        this.includeNegativeTests = include;
    }
    
    public void setIncludeEdgeCases(boolean include) {
        this.includeEdgeCases = include;
    }
    
    public void setIncludeParameterizedTests(boolean include) {
        this.includeParameterizedTests = include;
    }

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
                
                if (includeNegativeTests) {
                    sb.append(generateNegativeTest(classInfo, method));
                    sb.append("\n");
                }
                
                if (includeEdgeCases) {
                    sb.append(generateEdgeCaseTest(classInfo, method));
                    sb.append("\n");
                }
                
                if (includeParameterizedTests && hasSuitableParameters(method)) {
                    sb.append(generateParameterizedTest(classInfo, method));
                    sb.append("\n");
                }
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

    protected abstract String generatePackageDeclaration(ClassInfo classInfo);
    protected abstract String generateImports(ClassInfo classInfo);
    protected abstract String generateClassDeclaration(ClassInfo classInfo);
    protected abstract String generateFields(ClassInfo classInfo);
    protected abstract String generateSetupMethod(ClassInfo classInfo);
    protected abstract String generateTestMethod(ClassInfo classInfo, MethodInfo method);
    
    protected String generateNegativeTest(ClassInfo classInfo, MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        
        List<String> exceptionTypes = method.thrownExceptions();
        if (exceptionTypes.isEmpty() && !hasNullableParameters(method)) {
            return "";
        }
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should handle invalid input for ").append(method.name()).append("\")\n");
        sb.append("    void test").append(capitalize(method.name())).append("WithInvalidInput() {\n");
        
        if (!exceptionTypes.isEmpty()) {
            sb.append("        assertThrows(").append(exceptionTypes.get(0)).append(".class, () -> {\n");
            sb.append("            ").append(toCamelCase(classInfo.className())).append(".")
              .append(method.name()).append("(");
            sb.append(generateInvalidParameters(method));
            sb.append(");\n");
            sb.append("        });\n");
        } else {
            sb.append("        // Given - null input\n");
            sb.append("        // When/Then\n");
            sb.append("        assertDoesNotThrow(() -> {\n");
            sb.append("            ").append(toCamelCase(classInfo.className())).append(".")
              .append(method.name()).append("(");
            sb.append(generateNullParameters(method));
            sb.append(");\n");
            sb.append("        });\n");
        }
        
        sb.append("    }\n");
        
        return sb.toString();
    }
    
    protected String generateEdgeCaseTest(ClassInfo classInfo, MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        
        List<ParameterInfo> params = method.parameters();
        if (params.isEmpty()) {
            return "";
        }
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should handle edge cases for ").append(method.name()).append("\")\n");
        sb.append("    void test").append(capitalize(method.name())).append("EdgeCases() {\n");
        
        sb.append("        // Given - edge case values\n");
        for (ParameterInfo param : params) {
            sb.append("        ").append(param.type()).append(" ").append(param.name())
              .append(" = ").append(generateEdgeCaseValue(param.type())).append(";\n");
        }
        sb.append("\n");
        
        sb.append("        // When\n");
        if (!method.returnType().equals("void")) {
            sb.append("        ").append(method.returnType()).append(" result = ");
        }
        sb.append(toCamelCase(classInfo.className())).append(".")
          .append(method.name()).append("(");
        sb.append(params.stream().map(ParameterInfo::name).collect(Collectors.joining(", ")));
        sb.append(");\n\n");
        
        sb.append("        // Then\n");
        if (!method.returnType().equals("void")) {
            sb.append("        assertNotNull(result);\n");
        }
        
        sb.append("    }\n");
        
        return sb.toString();
    }
    
    protected String generateParameterizedTest(ClassInfo classInfo, MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        
        List<ParameterInfo> params = method.parameters();
        ParameterInfo targetParam = params.stream()
            .filter(p -> isSuitableForParameterization(p.type()))
            .findFirst()
            .orElse(null);
        
        if (targetParam == null) {
            return "";
        }
        
        sb.append("    @ParameterizedTest\n");
        sb.append("    @ValueSource(").append(getValueSourceAnnotation(targetParam.type())).append(")\n");
        sb.append("    @DisplayName(\"Should handle various inputs for ").append(method.name()).append("\")\n");
        sb.append("    void test").append(capitalize(method.name())).append("WithVariousInputs(")
          .append(targetParam.type()).append(" ").append(targetParam.name()).append(") {\n");
        
        sb.append("        // Given\n");
        for (ParameterInfo param : params) {
            if (!param.name().equals(targetParam.name())) {
                sb.append("        ").append(param.type()).append(" ").append(param.name())
                  .append(" = ").append(generateMockValue(param.type())).append(";\n");
            }
        }
        sb.append("\n");
        
        sb.append("        // When\n");
        if (!method.returnType().equals("void")) {
            sb.append("        ").append(method.returnType()).append(" result = ");
        }
        sb.append(toCamelCase(classInfo.className())).append(".")
          .append(method.name()).append("(");
        sb.append(params.stream().map(ParameterInfo::name).collect(Collectors.joining(", ")));
        sb.append(");\n\n");
        
        sb.append("        // Then\n");
        if (!method.returnType().equals("void")) {
            sb.append("        assertNotNull(result);\n");
        }
        
        sb.append("    }\n");
        
        return sb.toString();
    }
    
    protected String generateCoverageTest(ClassInfo classInfo, CoverageInfo info) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Test uncovered lines in ")
          .append(info.methodName()).append("\")\n");
        sb.append("    void test").append(capitalize(info.methodName()))
          .append("UncoveredLines() {\n");
        sb.append("        // TODO: Add test for uncovered lines: ")
          .append(info.lineNumber()).append("\n");
        sb.append("    }\n");
        
        return sb.toString();
    }
    
    protected boolean hasSuitableParameters(MethodInfo method) {
        return method.parameters().stream()
            .anyMatch(p -> isSuitableForParameterization(p.type()));
    }
    
    protected boolean isSuitableForParameterization(String type) {
        return type.equals("int") || type.equals("Integer") ||
               type.equals("long") || type.equals("Long") ||
               type.equals("String") || type.equals("double") || type.equals("Double");
    }
    
    protected boolean hasNullableParameters(MethodInfo method) {
        return !method.parameters().isEmpty();
    }
    
    protected String generateInvalidParameters(MethodInfo method) {
        return method.parameters().stream()
            .map(p -> "null")
            .collect(Collectors.joining(", "));
    }
    
    protected String generateNullParameters(MethodInfo method) {
        return method.parameters().stream()
            .map(p -> "null")
            .collect(Collectors.joining(", "));
    }
    
    protected String generateEdgeCaseValue(String type) {
        return switch (type) {
            case "int", "Integer" -> "Integer.MAX_VALUE";
            case "long", "Long" -> "Long.MAX_VALUE";
            case "double", "Double" -> "Double.MAX_VALUE";
            case "float", "Float" -> "Float.MAX_VALUE";
            case "String" -> "\"\"";
            case "boolean", "Boolean" -> "false";
            case "List", "ArrayList" -> "Collections.emptyList()";
            case "Set", "HashSet" -> "Collections.emptySet()";
            case "Map", "HashMap" -> "Collections.emptyMap()";
            default -> "null";
        };
    }
    
    protected String getValueSourceAnnotation(String type) {
        if (type.equals("int") || type.equals("Integer")) {
            return "ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE}";
        } else if (type.equals("long") || type.equals("Long")) {
            return "longs = {0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE}";
        } else if (type.equals("double") || type.equals("Double")) {
            return "doubles = {0.0, 1.0, -1.0, Double.MAX_VALUE}";
        } else if (type.equals("String")) {
            return "strings = {\"\", \"test\", \"a\", \"   \"}";
        }
        return "strings = {\"test\"}";
    }
    
    protected String generateMockValue(String type) {
        return switch (type) {
            case "String" -> "\"test\"";
            case "int", "Integer" -> "1";
            case "long", "Long" -> "1L";
            case "boolean", "Boolean" -> "true";
            case "double", "Double" -> "1.0";
            case "float", "Float" -> "1.0f";
            case "List", "ArrayList" -> "new ArrayList<>()";
            case "Set", "HashSet" -> "new HashSet<>()";
            case "Map", "HashMap" -> "new HashMap<>()";
            default -> "mock(" + type + ".class)";
        };
    }
    
    protected String toCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
    
    protected String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
