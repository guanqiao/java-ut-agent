package com.utagent.generator.strategy;

import com.utagent.model.AnnotationInfo;
import com.utagent.model.ClassInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;
import com.utagent.model.ParameterInfo;
import com.utagent.parser.FrameworkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpringBootTestStrategy Tests")
class SpringBootTestStrategyTest {

    private SpringBootTestStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SpringBootTestStrategy();
    }

    @Test
    @DisplayName("Should generate controller test with WebMvcTest")
    void shouldGenerateControllerTestWithWebMvcTest() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("getUser", "User",
            List.of(new ParameterInfo("id", "Long")),
            List.of(new AnnotationInfo("GetMapping", Map.of("value", "/users/{id}"))),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        List<FieldInfo> fields = new ArrayList<>();
        fields.add(new FieldInfo("userService", "UserService",
            List.of(new AnnotationInfo("Autowired")), false, false, false, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.controller",
            "UserController",
            "com.example.controller.UserController",
            methods,
            fields,
            List.of(new AnnotationInfo("RestController")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertNotNull(testCode);
        assertTrue(testCode.contains("@WebMvcTest"));
        assertTrue(testCode.contains("UserControllerTest"));
        assertTrue(testCode.contains("MockMvc"));
        assertTrue(testCode.contains("@MockBean"));
    }

    @Test
    @DisplayName("Should generate service test with MockitoExtension")
    void shouldGenerateServiceTestWithMockitoExtension() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("findById", "User",
            List.of(new ParameterInfo("id", "Long")),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        List<FieldInfo> fields = new ArrayList<>();
        fields.add(new FieldInfo("repository", "UserRepository",
            List.of(new AnnotationInfo("Autowired")), false, false, false, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.service",
            "UserService",
            "com.example.service.UserService",
            methods,
            fields,
            List.of(new AnnotationInfo("Service")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertNotNull(testCode);
        assertTrue(testCode.contains("@ExtendWith(MockitoExtension.class)"));
        assertTrue(testCode.contains("UserServiceTest"));
        assertTrue(testCode.contains("@InjectMocks"));
        assertTrue(testCode.contains("@Mock"));
    }

    @Test
    @DisplayName("Should generate repository test with DataJpaTest")
    void shouldGenerateRepositoryTestWithDataJpaTest() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("findByUsername", "User",
            List.of(new ParameterInfo("username", "String")),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.repository",
            "UserRepository",
            "com.example.repository.UserRepository",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Repository")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertNotNull(testCode);
        assertTrue(testCode.contains("@DataJpaTest"));
        assertTrue(testCode.contains("UserRepositoryTest"));
        assertTrue(testCode.contains("TestEntityManager"));
    }

    @Test
    @DisplayName("Should generate test method for GET endpoint")
    void shouldGenerateTestMethodForGetEndpoint() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("listUsers", "List<User>",
            new ArrayList<>(),
            List.of(new AnnotationInfo("GetMapping", Map.of("value", "/users"))),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.controller",
            "UserController",
            "com.example.controller.UserController",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("RestController")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("mockMvc.perform(get("));
        assertTrue(testCode.contains("/users"));
        assertTrue(testCode.contains("status().isOk()"));
    }

    @Test
    @DisplayName("Should generate test method for POST endpoint")
    void shouldGenerateTestMethodForPostEndpoint() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("createUser", "User",
            List.of(new ParameterInfo("user", "User")),
            List.of(new AnnotationInfo("PostMapping", Map.of("value", "/users"))),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.controller",
            "UserController",
            "com.example.controller.UserController",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("RestController")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("mockMvc.perform(post("));
        assertTrue(testCode.contains("/users"));
    }

    @Test
    @DisplayName("Should generate test method for PUT endpoint")
    void shouldGenerateTestMethodForPutEndpoint() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("updateUser", "User",
            List.of(new ParameterInfo("id", "Long"), new ParameterInfo("user", "User")),
            List.of(new AnnotationInfo("PutMapping", Map.of("value", "/users/{id}"))),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.controller",
            "UserController",
            "com.example.controller.UserController",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("RestController")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("mockMvc.perform(put("));
    }

    @Test
    @DisplayName("Should generate test method for DELETE endpoint")
    void shouldGenerateTestMethodForDeleteEndpoint() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("deleteUser", "void",
            List.of(new ParameterInfo("id", "Long")),
            List.of(new AnnotationInfo("DeleteMapping", Map.of("value", "/users/{id}"))),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.controller",
            "UserController",
            "com.example.controller.UserController",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("RestController")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("mockMvc.perform(delete("));
    }

    @Test
    @DisplayName("Should include required imports")
    void shouldIncludeRequiredImports() {
        String[] imports = strategy.getRequiredImports();

        assertNotNull(imports);
        assertTrue(imports.length > 0);
        assertTrue(List.of(imports).contains("org.junit.jupiter.api.Test"));
        assertTrue(List.of(imports).contains("org.springframework.boot.test.context.SpringBootTest"));
    }

    @Test
    @DisplayName("Should return Spring Boot as supported framework")
    void shouldReturnSpringBootAsSupportedFramework() {
        Set<FrameworkType> frameworks = strategy.getSupportedFrameworks();

        assertTrue(frameworks.contains(FrameworkType.SPRING_BOOT));
    }

    @Test
    @DisplayName("Should return @Test annotation")
    void shouldReturnTestAnnotation() {
        String annotation = strategy.getTestAnnotation();

        assertEquals("@Test", annotation);
    }

    @Test
    @DisplayName("Should generate test method with Given-When-Then structure")
    void shouldGenerateTestMethodWithGivenWhenThenStructure() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("process", "String",
            List.of(new ParameterInfo("input", "String")),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.service",
            "Processor",
            "com.example.service.Processor",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Service")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("// Given"));
        assertTrue(testCode.contains("// When"));
        assertTrue(testCode.contains("// Then"));
    }

    @Test
    @DisplayName("Should generate test for void method")
    void shouldGenerateTestForVoidMethod() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("doSomething", "void",
            new ArrayList<>(),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.service",
            "Service",
            "com.example.service.Service",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Service")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("void shouldDoSomethingSuccessfully()"));
    }

    @Test
    @DisplayName("Should skip private and abstract methods")
    void shouldSkipPrivateAndAbstractMethods() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("publicMethod", "void",
            new ArrayList<>(),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));
        methods.add(new MethodInfo("privateMethod", "void",
            new ArrayList<>(),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, true, false, false, false, false));
        methods.add(new MethodInfo("abstractMethod", "void",
            new ArrayList<>(),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, false, false, false, true, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.service",
            "Service",
            "com.example.service.Service",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Service")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("shouldPublicMethodSuccessfully"));
        assertFalse(testCode.contains("shouldPrivateMethodSuccessfully"));
        assertFalse(testCode.contains("shouldAbstractMethodSuccessfully"));
    }
}
