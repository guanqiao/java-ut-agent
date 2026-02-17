package com.utagent.generator.strategy;

import com.utagent.model.ClassInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;
import com.utagent.parser.FrameworkType;

import java.util.Set;

public class DubboTestStrategy extends EnhancedTestStrategy {

    @Override
    public Set<FrameworkType> getSupportedFrameworks() {
        return Set.of(FrameworkType.DUBBO);
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
        imports.append("import static org.junit.jupiter.api.Assertions.*;\n");
        imports.append("import static org.mockito.Mockito.*;\n");
        imports.append("import static org.mockito.ArgumentMatchers.*;\n");
        
        for (FieldInfo field : classInfo.fields()) {
            if (field.isDependencyInjection()) {
                if (field.hasAnnotation("DubboReference")) {
                    imports.append("import org.apache.dubbo.config.annotation.DubboReference;\n");
                    break;
                }
            }
        }
        
        return imports.toString();
    }

    @Override
    protected String generateClassDeclaration(ClassInfo classInfo) {
        return "@DisplayName(\"" + classInfo.className() + " Tests\")\n" +
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
        
        test.append("    @Test\n");
        test.append("    @DisplayName(\"Should execute ").append(method.name()).append(" successfully\")\n");
        test.append("    void test").append(capitalize(method.name())).append("() {\n");
        
        test.append("        // Given\n");
        for (var param : method.parameters()) {
            test.append("        ").append(param.type()).append(" ").append(param.name())
                .append(" = ").append(generateMockValue(param.type())).append(";\n");
        }
        test.append("\n");
        
        test.append("        // When\n");
        if (!method.returnType().equals("void")) {
            test.append("        ").append(method.returnType()).append(" result = ");
        }
        test.append(toCamelCase(classInfo.className())).append(".")
              .append(method.name()).append("(");
        test.append(String.join(", ", method.parameters().stream()
            .map(p -> p.name()).toList()));
        test.append(");\n\n");
        
        test.append("        // Then\n");
        if (!method.returnType().equals("void")) {
            test.append("        assertNotNull(result);\n");
        }
        
        test.append("    }\n");
        
        return test.toString();
    }
}
