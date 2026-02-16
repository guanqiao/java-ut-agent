package com.utagent.generator.strategy;

import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;
import com.utagent.parser.FrameworkType;

import java.util.List;
import java.util.Set;

public class SpringBootTestStrategy implements TestGenerationStrategy {

    private final SpringMvcTestStrategy baseStrategy = new SpringMvcTestStrategy();

    @Override
    public String generateTestClass(ClassInfo classInfo) {
        if (isController(classInfo)) {
            return generateControllerTest(classInfo);
        } else if (isService(classInfo)) {
            return generateServiceTest(classInfo);
        } else if (isRepository(classInfo)) {
            return generateRepositoryTest(classInfo);
        }
        return baseStrategy.generateTestClass(classInfo);
    }

    @Override
    public String generateTestMethod(ClassInfo classInfo, String methodName, List<String> uncoveredLines) {
        return baseStrategy.generateTestMethod(classInfo, methodName, uncoveredLines);
    }

    @Override
    public String generateAdditionalTests(ClassInfo classInfo, List<CoverageInfo> coverageInfo) {
        return baseStrategy.generateAdditionalTests(classInfo, coverageInfo);
    }

    @Override
    public Set<FrameworkType> getSupportedFrameworks() {
        return Set.of(FrameworkType.SPRING_BOOT);
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
            "org.springframework.beans.factory.annotation.Autowired",
            "org.springframework.boot.test.context.SpringBootTest",
            "org.springframework.boot.test.mock.mockito.MockBean",
            "org.springframework.test.context.ActiveProfiles",
            "static org.junit.jupiter.api.Assertions.*",
            "static org.mockito.Mockito.*"
        };
    }

    private String generateControllerTest(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(generatePackageDeclaration(classInfo));
        sb.append("\n");
        sb.append(generateControllerImports(classInfo));
        sb.append("\n");
        sb.append(generateControllerClassDeclaration(classInfo));
        sb.append("\n");
        sb.append(generateControllerFields(classInfo));
        sb.append("\n");
        
        for (MethodInfo method : classInfo.methods()) {
            if (!method.isPrivate() && !method.isAbstract() && isRequestMapping(method)) {
                sb.append(generateControllerTestMethod(classInfo, method));
                sb.append("\n");
            }
        }
        
        sb.append("}\n");
        
        return sb.toString();
    }

    private String generateServiceTest(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(generatePackageDeclaration(classInfo));
        sb.append("\n");
        sb.append(generateServiceImports(classInfo));
        sb.append("\n");
        sb.append(generateServiceClassDeclaration(classInfo));
        sb.append("\n");
        sb.append(generateServiceFields(classInfo));
        sb.append("\n");
        sb.append(generateSetupMethod());
        sb.append("\n");
        
        for (MethodInfo method : classInfo.methods()) {
            if (!method.isPrivate() && !method.isAbstract()) {
                sb.append(generateServiceTestMethod(classInfo, method));
                sb.append("\n");
            }
        }
        
        sb.append("}\n");
        
        return sb.toString();
    }

    private String generateRepositoryTest(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(generatePackageDeclaration(classInfo));
        sb.append("\n");
        sb.append(generateRepositoryImports(classInfo));
        sb.append("\n");
        sb.append(generateRepositoryClassDeclaration(classInfo));
        sb.append("\n");
        sb.append(generateRepositoryFields(classInfo));
        sb.append("\n");
        
        for (MethodInfo method : classInfo.methods()) {
            if (!method.isPrivate() && !method.isAbstract()) {
                sb.append(generateRepositoryTestMethod(classInfo, method));
                sb.append("\n");
            }
        }
        
        sb.append("}\n");
        
        return sb.toString();
    }

    private boolean isController(ClassInfo classInfo) {
        return classInfo.hasAnnotation("Controller") || 
               classInfo.hasAnnotation("RestController");
    }

    private boolean isService(ClassInfo classInfo) {
        return classInfo.hasAnnotation("Service");
    }

    private boolean isRepository(ClassInfo classInfo) {
        return classInfo.hasAnnotation("Repository") ||
               classInfo.fullyQualifiedName().endsWith("Repository");
    }

    private boolean isRequestMapping(MethodInfo method) {
        return method.hasAnnotation("RequestMapping") ||
               method.hasAnnotation("GetMapping") ||
               method.hasAnnotation("PostMapping") ||
               method.hasAnnotation("PutMapping") ||
               method.hasAnnotation("DeleteMapping") ||
               method.hasAnnotation("PatchMapping");
    }

    private String generatePackageDeclaration(ClassInfo classInfo) {
        return "package " + classInfo.packageName() + ";\n";
    }

    private String generateControllerImports(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        sb.append("import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;\n");
        sb.append("import org.springframework.boot.test.mock.mockito.MockBean;\n");
        sb.append("import org.springframework.test.web.servlet.MockMvc;\n");
        sb.append("import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;\n");
        sb.append("import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;\n");
        sb.append("import static org.mockito.Mockito.*;\n");
        sb.append("import ").append(classInfo.fullyQualifiedName()).append(";\n");
        return sb.toString();
    }

    private String generateControllerClassDeclaration(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("@WebMvcTest(").append(classInfo.className()).append(".class)\n");
        sb.append("@DisplayName(\"").append(classInfo.className()).append(" Controller Tests\")\n");
        sb.append("class ").append(classInfo.className()).append("Test {\n");
        return sb.toString();
    }

    private String generateControllerFields(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("    @Autowired\n");
        sb.append("    private MockMvc mockMvc;\n\n");
        
        for (FieldInfo field : classInfo.fields()) {
            if (field.isDependencyInjection()) {
                sb.append("    @MockBean\n");
                sb.append("    private ").append(field.type()).append(" ")
                  .append(toCamelCase(field.type())).append(";\n");
            }
        }
        return sb.toString();
    }

    private String generateControllerTestMethod(ClassInfo classInfo, MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        
        String httpMethod = getHttpMethod(method);
        String path = getRequestPath(method);
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should handle ").append(httpMethod)
          .append(" request to ").append(path).append("\")\n");
        sb.append("    void should").append(capitalize(method.name())).append("() throws Exception {\n");
        sb.append("        // Given - Setup mock behavior for dependencies\n");
        sb.append("        // when(mockDependency.method()).thenReturn(expectedValue);\n\n");
        sb.append("        // When & Then\n");
        sb.append("        mockMvc.perform(").append(httpMethod).append("(\"")
          .append(path).append("\"))\n");
        sb.append("            .andExpect(status().isOk());\n");
        sb.append("    }\n");
        
        return sb.toString();
    }

    private String generateServiceImports(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.BeforeEach;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import org.junit.jupiter.api.extension.ExtendWith;\n");
        sb.append("import org.mockito.InjectMocks;\n");
        sb.append("import org.mockito.Mock;\n");
        sb.append("import org.mockito.junit.jupiter.MockitoExtension;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n");
        sb.append("import static org.mockito.Mockito.*;\n");
        sb.append("import ").append(classInfo.fullyQualifiedName()).append(";\n");
        return sb.toString();
    }

    private String generateServiceClassDeclaration(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("@ExtendWith(MockitoExtension.class)\n");
        sb.append("@DisplayName(\"").append(classInfo.className()).append(" Service Tests\")\n");
        sb.append("class ").append(classInfo.className()).append("Test {\n");
        return sb.toString();
    }

    private String generateServiceFields(ClassInfo classInfo) {
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

    private String generateSetupMethod() {
        return "    @BeforeEach\n" +
               "    void setUp() {\n" +
               "        // Setup test data\n" +
               "    }\n";
    }

    private String generateServiceTestMethod(ClassInfo classInfo, MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should ").append(method.name())
          .append(" successfully\")\n");
        sb.append("    void should").append(capitalize(method.name())).append("Successfully() {\n");
        sb.append("        // Given - Prepare test data and mock behavior\n");
        sb.append("        // var input = new InputType();\n");
        sb.append("        // when(mockDependency.method()).thenReturn(expectedValue);\n\n");
        sb.append("        // When\n");
        if (!method.returnType().equals("void")) {
            sb.append("        var result = ");
        }
        sb.append(toCamelCase(classInfo.className())).append(".")
          .append(method.name()).append("();\n\n");
        sb.append("        // Then\n");
        if (!method.returnType().equals("void")) {
            sb.append("        assertNotNull(result);\n");
        }
        sb.append("    }\n");
        
        return sb.toString();
    }

    private String generateRepositoryImports(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import org.junit.jupiter.api.DisplayName;\n");
        sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        sb.append("import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;\n");
        sb.append("import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n");
        sb.append("import ").append(classInfo.fullyQualifiedName()).append(";\n");
        return sb.toString();
    }

    private String generateRepositoryClassDeclaration(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("@DataJpaTest\n");
        sb.append("@DisplayName(\"").append(classInfo.className()).append(" Repository Tests\")\n");
        sb.append("class ").append(classInfo.className()).append("Test {\n");
        return sb.toString();
    }

    private String generateRepositoryFields(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("    @Autowired\n");
        sb.append("    private TestEntityManager entityManager;\n\n");
        sb.append("    @Autowired\n");
        sb.append("    private ").append(classInfo.className()).append(" ")
          .append(toCamelCase(classInfo.className())).append(";\n");
        return sb.toString();
    }

    private String generateRepositoryTestMethod(ClassInfo classInfo, MethodInfo method) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("    @Test\n");
        sb.append("    @DisplayName(\"Should ").append(method.name())
          .append(" successfully\")\n");
        sb.append("    void should").append(capitalize(method.name())).append("Successfully() {\n");
        sb.append("        // Given - Prepare test entity\n");
        sb.append("        // var entity = new Entity();\n");
        sb.append("        // entityManager.persist(entity);\n\n");
        sb.append("        // When\n");
        if (!method.returnType().equals("void")) {
            sb.append("        var result = ");
        }
        sb.append(toCamelCase(classInfo.className())).append(".")
          .append(method.name()).append("();\n\n");
        sb.append("        // Then\n");
        if (!method.returnType().equals("void")) {
            sb.append("        assertNotNull(result);\n");
        }
        sb.append("    }\n");
        
        return sb.toString();
    }

    private String getHttpMethod(MethodInfo method) {
        if (method.hasAnnotation("GetMapping")) return "get";
        if (method.hasAnnotation("PostMapping")) return "post";
        if (method.hasAnnotation("PutMapping")) return "put";
        if (method.hasAnnotation("DeleteMapping")) return "delete";
        if (method.hasAnnotation("PatchMapping")) return "patch";
        return "get";
    }

    private String getRequestPath(MethodInfo method) {
        for (var annotation : method.annotations()) {
            if (annotation.name().contains("Mapping")) {
                Object value = annotation.getAttribute("value");
                if (value != null) {
                    String path = value.toString();
                    if (path.startsWith("\"") && path.endsWith("\"")) {
                        return path.substring(1, path.length() - 1);
                    }
                    return path;
                }
            }
        }
        return "/";
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
