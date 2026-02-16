package com.utagent.testdata;

import com.utagent.model.MethodInfo;
import com.utagent.model.ParameterInfo;

import java.util.ArrayList;
import java.util.List;

public class ParameterizedTestCase {

    private final MethodInfo method;
    private final String testMethodName;
    private final String dataSource;
    private final List<Object[]> testArguments;
    private boolean parameterized;

    public ParameterizedTestCase(MethodInfo method) {
        this.method = method;
        this.testMethodName = generateTestMethodName();
        this.dataSource = determineDataSource();
        this.testArguments = new ArrayList<>();
        this.parameterized = !method.parameters().isEmpty();
        generateTestArguments();
    }

    private String generateTestMethodName() {
        return "should" + capitalize(method.name()) + "WithVariousInputs";
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    private String determineDataSource() {
        if (method.parameters().isEmpty()) {
            return "";
        }
        
        if (method.parameters().size() == 1) {
            ParameterInfo param = method.parameters().get(0);
            if (isSimpleType(param.type())) {
                return "@ValueSource";
            }
        }
        
        if (method.parameters().size() <= 3 && allSimpleTypes()) {
            return "@CsvSource";
        }
        
        return "@MethodSource";
    }

    private boolean isSimpleType(String type) {
        return type.equals("int") || type.equals("Integer") ||
               type.equals("long") || type.equals("Long") ||
               type.equals("double") || type.equals("Double") ||
               type.equals("boolean") || type.equals("Boolean") ||
               type.equals("String") || type.equals("char") ||
               type.equals("Character");
    }

    private boolean allSimpleTypes() {
        return method.parameters().stream()
            .allMatch(p -> isSimpleType(p.type()));
    }

    private void generateTestArguments() {
        BoundaryValueGenerator boundaryGen = new BoundaryValueGenerator();
        
        for (ParameterInfo param : method.parameters()) {
            Class<?> paramType = resolveType(param.type());
            List<Object> values = boundaryGen.generate(paramType);
            testArguments.add(values.toArray());
        }
    }

    private Class<?> resolveType(String typeName) {
        return switch (typeName) {
            case "int" -> int.class;
            case "long" -> long.class;
            case "double" -> double.class;
            case "boolean" -> boolean.class;
            case "String" -> String.class;
            default -> Object.class;
        };
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public String getDataSource() {
        return dataSource;
    }

    public boolean isParameterized() {
        return parameterized;
    }

    public String generateTestCode() {
        StringBuilder sb = new StringBuilder();
        
        if (!parameterized) {
            sb.append("    @Test\n");
            sb.append("    @DisplayName(\"Should ").append(method.name()).append("\")\n");
            sb.append("    void ").append(testMethodName).append("() {\n");
            sb.append("        // TODO: Implement test\n");
            sb.append("    }\n");
            return sb.toString();
        }
        
        sb.append("    @ParameterizedTest\n");
        sb.append("    @DisplayName(\"Should ").append(method.name()).append(" with various inputs\")\n");
        
        if (dataSource.equals("@ValueSource")) {
            generateValueSourceAnnotation(sb);
        } else if (dataSource.equals("@CsvSource")) {
            generateCsvSourceAnnotation(sb);
        } else {
            sb.append("    @MethodSource(\"").append(testMethodName).append("Provider\")\n");
        }
        
        sb.append("    void ").append(testMethodName).append("(");
        sb.append(generateParameterList());
        sb.append(") {\n");
        sb.append("        // Act\n");
        sb.append("        var result = ").append(method.name()).append("(");
        sb.append(generateArgumentList());
        sb.append(");\n\n");
        sb.append("        // Assert\n");
        sb.append("        assertThat(result).isNotNull();\n");
        sb.append("    }\n");
        
        return sb.toString();
    }

    private void generateValueSourceAnnotation(StringBuilder sb) {
        ParameterInfo param = method.parameters().get(0);
        sb.append("    @ValueSource(");
        
        if (param.type().equals("int") || param.type().equals("Integer")) {
            sb.append("ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE}");
        } else if (param.type().equals("String")) {
            sb.append("strings = {\"\", \"test\", \"a\", \"longer test string\"}");
        } else if (param.type().equals("boolean") || param.type().equals("Boolean")) {
            sb.append("booleans = {true, false}");
        } else if (param.type().equals("long") || param.type().equals("Long")) {
            sb.append("longs = {0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE}");
        } else if (param.type().equals("double") || param.type().equals("Double")) {
            sb.append("doubles = {0.0, 1.0, -1.0, Double.MAX_VALUE, Double.MIN_VALUE}");
        }
        
        sb.append(")\n");
    }

    private void generateCsvSourceAnnotation(StringBuilder sb) {
        sb.append("    @CsvSource({\n");
        sb.append("        ''test1, test2',\n");
        sb.append("        ''test3, test4'\n");
        sb.append("    })\n");
    }

    private String generateParameterList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < method.parameters().size(); i++) {
            if (i > 0) sb.append(", ");
            ParameterInfo param = method.parameters().get(i);
            sb.append(param.type()).append(" ").append(param.name());
        }
        return sb.toString();
    }

    private String generateArgumentList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < method.parameters().size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(method.parameters().get(i).name());
        }
        return sb.toString();
    }

    public String generateProviderMethod() {
        if (!dataSource.equals("@MethodSource")) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("    static Stream<Arguments> ").append(testMethodName).append("Provider() {\n");
        sb.append("        return Stream.of(\n");
        sb.append("            arguments(/* test data 1 */),\n");
        sb.append("            arguments(/* test data 2 */)\n");
        sb.append("        );\n");
        sb.append("    }\n");
        
        return sb.toString();
    }
}
