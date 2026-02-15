package com.utagent.generator.strategy;

import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.model.MethodInfo;
import com.utagent.parser.FrameworkType;

import java.util.List;
import java.util.Set;

public class MyBatisPlusTestStrategy implements TestGenerationStrategy {

    private final MyBatisTestStrategy baseStrategy = new MyBatisTestStrategy();

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
        
        sb.append(generateBaseMapperTests(classInfo));
        sb.append("}\n");
        
        return sb.toString();
    }

    @Override
    public String generateTestMethod(ClassInfo classInfo, String methodName, List<String> uncoveredLines) {
        return baseStrategy.generateTestMethod(classInfo, methodName, uncoveredLines);
    }

    @Override
    public String generateAdditionalTests(ClassInfo classInfo, List<CoverageInfo> coverageInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(baseStrategy.generateAdditionalTests(classInfo, coverageInfo));
        
        sb.append(generatePaginationTests(classInfo));
        sb.append(generateConditionTests(classInfo));
        
        return sb.toString();
    }

    @Override
    public Set<FrameworkType> getSupportedFrameworks() {
        return Set.of(FrameworkType.MYBATIS_PLUS);
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
            "com.baomidou.mybatisplus.core.conditions.query.QueryWrapper",
            "com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper",
            "com.baomidou.mybatisplus.extension.plugins.pagination.Page",
            "org.springframework.beans.factory.annotation.Autowired",
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
        sb.append("import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;\n");
        sb.append("import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;\n");
        sb.append("import com.baomidou.mybatisplus.extension.plugins.pagination.Page;\n");
        sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n");
        sb.append("import ").append(classInfo.fullyQualifiedName()).append(";\n");
        
        return sb.toString();
    }

    private String generateClassDeclaration(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
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
        return generateCustomMapperTestMethod(classInfo, method);
    }

    private String generateCustomMapperTestMethod(ClassInfo classInfo, MethodInfo method) {
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
        
        if (!method.parameters().isEmpty()) {
            sb.append("\n");
        }
        
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
        if (!method.returnType().equals("void")) {
            sb.append("        assertNotNull(result);\n");
        }
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
            default -> {
                if (type.endsWith("Id") || type.endsWith("ID")) {
                    yield "1L";
                }
                yield "null";
            }
        };
    }

    private String generateBaseMapperTests(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        String entityName = toCamelCase(classInfo.className().replace("Mapper", "").replace("Dao", ""));
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should select by ID successfully\")\n");
        sb.append("    void shouldSelectByIdSuccessfully() {\n");
        sb.append("        // Given\n");
        sb.append("        Long id = 1L;\n\n");
        sb.append("        // When\n");
        sb.append("        var result = ").append(toCamelCase(classInfo.className()))
          .append(".selectById(id);\n\n");
        sb.append("        // Then\n");
        sb.append("        // Verify result\n");
        sb.append("    }\n\n");
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should select list with wrapper successfully\")\n");
        sb.append("    void shouldSelectListWithWrapperSuccessfully() {\n");
        sb.append("        // Given\n");
        sb.append("        QueryWrapper<").append(capitalize(entityName))
          .append("> wrapper = new QueryWrapper<>();\n");
        sb.append("        wrapper.eq(\"status\", 1);\n\n");
        sb.append("        // When\n");
        sb.append("        var result = ").append(toCamelCase(classInfo.className()))
          .append(".selectList(wrapper);\n\n");
        sb.append("        // Then\n");
        sb.append("        assertNotNull(result);\n");
        sb.append("    }\n\n");
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should select page successfully\")\n");
        sb.append("    void shouldSelectPageSuccessfully() {\n");
        sb.append("        // Given\n");
        sb.append("        Page<").append(capitalize(entityName))
          .append("> page = new Page<>(1, 10);\n");
        sb.append("        QueryWrapper<").append(capitalize(entityName))
          .append("> wrapper = new QueryWrapper<>();\n\n");
        sb.append("        // When\n");
        sb.append("        var result = ").append(toCamelCase(classInfo.className()))
          .append(".selectPage(page, wrapper);\n\n");
        sb.append("        // Then\n");
        sb.append("        assertNotNull(result);\n");
        sb.append("    }\n\n");
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should insert successfully\")\n");
        sb.append("    void shouldInsertSuccessfully() {\n");
        sb.append("        // Given\n");
        sb.append("        ").append(capitalize(entityName)).append(" entity = new ")
          .append(capitalize(entityName)).append("();\n");
        sb.append("        // Set entity properties\n\n");
        sb.append("        // When\n");
        sb.append("        int result = ").append(toCamelCase(classInfo.className()))
          .append(".insert(entity);\n\n");
        sb.append("        // Then\n");
        sb.append("        assertTrue(result > 0);\n");
        sb.append("    }\n\n");
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should update by wrapper successfully\")\n");
        sb.append("    void shouldUpdateByWrapperSuccessfully() {\n");
        sb.append("        // Given\n");
        sb.append("        UpdateWrapper<").append(capitalize(entityName))
          .append("> wrapper = new UpdateWrapper<>();\n");
        sb.append("        wrapper.eq(\"id\", 1L).set(\"status\", 0);\n\n");
        sb.append("        // When\n");
        sb.append("        int result = ").append(toCamelCase(classInfo.className()))
          .append(".update(null, wrapper);\n\n");
        sb.append("        // Then\n");
        sb.append("        assertTrue(result >= 0);\n");
        sb.append("    }\n\n");
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should delete by ID successfully\")\n");
        sb.append("    void shouldDeleteByIdSuccessfully() {\n");
        sb.append("        // Given\n");
        sb.append("        Long id = 1L;\n\n");
        sb.append("        // When\n");
        sb.append("        int result = ").append(toCamelCase(classInfo.className()))
          .append(".deleteById(id);\n\n");
        sb.append("        // Then\n");
        sb.append("        assertTrue(result >= 0);\n");
        sb.append("    }\n");
        
        return sb.toString();
    }

    private String generatePaginationTests(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        String entityName = classInfo.className().replace("Mapper", "").replace("Dao", "");
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should handle pagination with large page number\")\n");
        sb.append("    void shouldHandlePaginationWithLargePageNumber() {\n");
        sb.append("        // Given\n");
        sb.append("        Page<").append(entityName).append("> page = new Page<>(1000, 10);\n\n");
        sb.append("        // When\n");
        sb.append("        var result = ").append(toCamelCase(classInfo.className()))
          .append(".selectPage(page, null);\n\n");
        sb.append("        // Then\n");
        sb.append("        assertNotNull(result);\n");
        sb.append("        assertTrue(result.getRecords().isEmpty());\n");
        sb.append("    }\n");
        
        return sb.toString();
    }

    private String generateConditionTests(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        String entityName = classInfo.className().replace("Mapper", "").replace("Dao", "");
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should handle complex query conditions\")\n");
        sb.append("    void shouldHandleComplexQueryConditions() {\n");
        sb.append("        // Given\n");
        sb.append("        QueryWrapper<").append(entityName).append("> wrapper = new QueryWrapper<>();\n");
        sb.append("        wrapper.and(w -> w.eq(\"status\", 1).or().eq(\"status\", 2))\n");
        sb.append("               .orderByDesc(\"create_time\")\n");
        sb.append("               .last(\"LIMIT 100\");\n\n");
        sb.append("        // When\n");
        sb.append("        var result = ").append(toCamelCase(classInfo.className()))
          .append(".selectList(wrapper);\n\n");
        sb.append("        // Then\n");
        sb.append("        assertNotNull(result);\n");
        sb.append("    }\n");
        
        return sb.toString();
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
