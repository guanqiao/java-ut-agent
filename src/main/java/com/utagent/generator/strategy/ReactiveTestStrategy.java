package com.utagent.generator.strategy;

import com.utagent.model.ClassInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;
import com.utagent.model.ParameterInfo;
import com.utagent.parser.FrameworkType;

import java.util.Set;
import java.util.stream.Collectors;

public class ReactiveTestStrategy extends EnhancedTestStrategy {

    @Override
    public Set<FrameworkType> getSupportedFrameworks() {
        return Set.of(FrameworkType.REACTIVE);
    }

    @Override
    protected String generatePackageDeclaration(ClassInfo classInfo) {
        return "package " + classInfo.packageName() + ";";
    }

    @Override
    protected String generateImports(ClassInfo classInfo) {
        StringBuilder imports = new StringBuilder();
        imports.append("import org.junit.jupiter.api.*;\n");
        imports.append("import org.mockito.*;\n");
        imports.append("import reactor.core.publisher.*;\n");
        imports.append("import reactor.test.StepVerifier;\n");
        imports.append("import static org.junit.jupiter.api.Assertions.*;\n");
        imports.append("import static org.mockito.Mockito.*;\n");
        imports.append("import static org.mockito.ArgumentMatchers.*;\n");
        
        return imports.toString();
    }

    @Override
    protected String generateClassDeclaration(ClassInfo classInfo) {
        return "@DisplayName(\"" + classInfo.className() + " Reactive Tests\")\n" +
               "class " + classInfo.className() + "Test {";
    }

    @Override
    protected String generateFields(ClassInfo classInfo) {
        StringBuilder fields = new StringBuilder();
        
        fields.append("    @InjectMocks\n");
        fields.append("    private ").append(classInfo.className()).append(" ")
              .append(toCamelCase(classInfo.className())).append(";\n\n");
        
        for (FieldInfo field : classInfo.fields()) {
            if (field.isDependencyInjection()) {
                fields.append("    @Mock\n");
                fields.append("    private ").append(field.type()).append(" ")
                      .append(field.name()).append(";\n");
            }
        }
        
        return fields.toString();
    }

    @Override
    protected String generateSetupMethod(ClassInfo classInfo) {
        return "    @BeforeEach\n" +
               "    void setUp() {\n" +
               "        MockitoAnnotations.openMocks(this);\n" +
               "    }\n";
    }

    @Override
    protected String generateTestMethod(ClassInfo classInfo, MethodInfo method) {
        StringBuilder test = new StringBuilder();
        
        boolean isReactive = isReactiveMethod(method);
        
        test.append("    @Test\n");
        test.append("    @DisplayName(\"Should execute ").append(method.name()).append(" successfully\")\n");
        test.append("    void test").append(capitalize(method.name())).append("() {\n");
        
        test.append("        // Given\n");
        for (var param : method.parameters()) {
            test.append("        ").append(param.type()).append(" ").append(param.name())
                .append(" = ").append(generateReactiveMockValue(param.type())).append(";\n");
        }
        test.append("\n");
        
        test.append("        // When\n");
        if (!method.returnType().equals("void")) {
            test.append("        ").append(method.returnType()).append(" result = ");
        }
        test.append(toCamelCase(classInfo.className())).append(".")
              .append(method.name()).append("(");
        test.append(method.parameters().stream()
            .map(ParameterInfo::name)
            .collect(Collectors.joining(", ")));
        test.append(");\n\n");
        
        test.append("        // Then\n");
        if (isReactive && !method.returnType().equals("void")) {
            test.append("        StepVerifier.create(result)\n");
            test.append("            .expectNextCount(1)\n");
            test.append("            .verifyComplete();\n");
        } else if (!method.returnType().equals("void")) {
            test.append("        assertNotNull(result);\n");
        }
        
        test.append("    }\n");
        
        return test.toString();
    }
    
    private boolean isReactiveMethod(MethodInfo method) {
        String returnType = method.returnType();
        return returnType.contains("Mono") || returnType.contains("Flux") ||
               returnType.contains("Publisher") || returnType.contains("Flowable") ||
               returnType.contains("Observable") || returnType.contains("Single") ||
               returnType.contains("Completable");
    }
    
    private String generateReactiveMockValue(String type) {
        if (type.contains("Mono")) {
            return "Mono.just(" + generateMockValueForGeneric(type) + ")";
        } else if (type.contains("Flux")) {
            return "Flux.just(" + generateMockValueForGeneric(type) + ")";
        } else if (type.contains("Publisher")) {
            return "Mono.just(" + generateMockValueForGeneric(type) + ")";
        }
        return generateMockValue(type);
    }
    
    private String generateMockValueForGeneric(String type) {
        if (type.contains("String")) {
            return "\"test\"";
        } else if (type.contains("Integer") || type.contains("int")) {
            return "1";
        } else if (type.contains("Long") || type.contains("long")) {
            return "1L";
        } else if (type.contains("Boolean") || type.contains("boolean")) {
            return "true";
        } else if (type.contains("List")) {
            return "java.util.Collections.emptyList()";
        }
        return "mock(" + extractGenericType(type) + ".class)";
    }
    
    private String extractGenericType(String type) {
        int start = type.indexOf("<");
        int end = type.lastIndexOf(">");
        if (start > 0 && end > start) {
            return type.substring(start + 1, end);
        }
        return "Object";
    }
}
