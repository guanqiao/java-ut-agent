package com.utagent.generator.strategy;

import com.utagent.model.AnnotationInfo;
import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
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

@DisplayName("MyBatisTestStrategy Tests")
class MyBatisTestStrategyTest {

    private MyBatisTestStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MyBatisTestStrategy();
    }

    @Test
    @DisplayName("Should generate test class with MybatisTest annotation")
    void shouldGenerateTestClassWithMybatisTestAnnotation() {
        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertNotNull(testCode);
        assertTrue(testCode.contains("@MybatisTest"));
        assertTrue(testCode.contains("UserMapperTest"));
        assertTrue(testCode.contains("@AutoConfigureTestDatabase"));
        assertTrue(testCode.contains("@ActiveProfiles(\"test\")"));
    }

    @Test
    @DisplayName("Should generate test method for select operation")
    void shouldGenerateTestMethodForSelectOperation() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("findById", "User",
            List.of(new ParameterInfo("id", "Long")),
            List.of(new AnnotationInfo("Select", Map.of("value", "SELECT * FROM users WHERE id = #{id}"))),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("shouldFindByIdSuccessfully"));
        assertTrue(testCode.contains("// Given"));
        assertTrue(testCode.contains("// When"));
        assertTrue(testCode.contains("// Then"));
        assertTrue(testCode.contains("assertNotNull(result)"));
    }

    @Test
    @DisplayName("Should generate test method for insert operation")
    void shouldGenerateTestMethodForInsertOperation() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("insert", "int",
            List.of(new ParameterInfo("user", "User")),
            List.of(new AnnotationInfo("Insert", Map.of("value", "INSERT INTO users ..."))),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("shouldInsertSuccessfully"));
        assertTrue(testCode.contains("// Verify the record was inserted"));
    }

    @Test
    @DisplayName("Should generate test method for update operation")
    void shouldGenerateTestMethodForUpdateOperation() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("update", "int",
            List.of(new ParameterInfo("user", "User")),
            List.of(new AnnotationInfo("Update", Map.of("value", "UPDATE users ..."))),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("shouldUpdateSuccessfully"));
        assertTrue(testCode.contains("// Verify the record was updated"));
    }

    @Test
    @DisplayName("Should generate test method for delete operation")
    void shouldGenerateTestMethodForDeleteOperation() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("deleteById", "int",
            List.of(new ParameterInfo("id", "Long")),
            List.of(new AnnotationInfo("Delete", Map.of("value", "DELETE FROM users WHERE id = #{id}"))),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("shouldDeleteByIdSuccessfully"));
        assertTrue(testCode.contains("// Verify the record was deleted"));
    }

    @Test
    @DisplayName("Should generate test method with method name detection")
    void shouldGenerateTestMethodWithMethodNameDetection() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("queryUsers", "List<User>",
            new ArrayList<>(),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("shouldQueryUsersSuccessfully"));
    }

    @Test
    @DisplayName("Should include required imports")
    void shouldIncludeRequiredImports() {
        String[] imports = strategy.getRequiredImports();

        assertNotNull(imports);
        assertTrue(imports.length > 0);
        assertTrue(List.of(imports).contains("org.junit.jupiter.api.Test"));
        assertTrue(List.of(imports).contains("org.mybatis.spring.boot.test.autoconfigure.MybatisTest"));
        assertTrue(List.of(imports).contains("org.springframework.beans.factory.annotation.Autowired"));
    }

    @Test
    @DisplayName("Should return MyBatis as supported framework")
    void shouldReturnMyBatisAsSupportedFramework() {
        Set<FrameworkType> frameworks = strategy.getSupportedFrameworks();

        assertTrue(frameworks.contains(FrameworkType.MYBATIS));
    }

    @Test
    @DisplayName("Should return @Test annotation")
    void shouldReturnTestAnnotation() {
        String annotation = strategy.getTestAnnotation();

        assertEquals("@Test", annotation);
    }

    @Test
    @DisplayName("Should generate additional tests for uncovered code")
    void shouldGenerateAdditionalTestsForUncoveredCode() {
        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        List<CoverageInfo> coverageInfo = new ArrayList<>();
        coverageInfo.add(new CoverageInfo(
            "com.example.mapper.UserMapper",
            "findById",
            10,
            2, 1,
            10, 5,
            5, 2
        ));

        String additionalTests = strategy.generateAdditionalTests(classInfo, coverageInfo);

        assertNotNull(additionalTests);
        assertTrue(additionalTests.contains("Test edge case for findById"));
        assertTrue(additionalTests.contains("testFindByIdEdgeCase"));
    }

    @Test
    @DisplayName("Should generate test method for specific method")
    void shouldGenerateTestMethodForSpecificMethod() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("findByUsername", "User",
            List.of(new ParameterInfo("username", "String")),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestMethod(classInfo, "findByUsername", List.of("15", "16"));

        assertNotNull(testCode);
        assertTrue(testCode.contains("shouldFindByUsernameSuccessfully"));
    }

    @Test
    @DisplayName("Should return empty string for non-existent method")
    void shouldReturnEmptyStringForNonExistentMethod() {
        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestMethod(classInfo, "nonExistentMethod", List.of());

        assertEquals("", testCode);
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
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("shouldPublicMethodSuccessfully"));
        assertFalse(testCode.contains("shouldPrivateMethodSuccessfully"));
        assertFalse(testCode.contains("shouldAbstractMethodSuccessfully"));
    }

    @Test
    @DisplayName("Should generate mock values for different parameter types")
    void shouldGenerateMockValuesForDifferentParameterTypes() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("complexQuery", "List<User>",
            List.of(
                new ParameterInfo("name", "String"),
                new ParameterInfo("age", "int"),
                new ParameterInfo("active", "boolean"),
                new ParameterInfo("salary", "double")
            ),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            methods,
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("\"test_value\""));
        assertTrue(testCode.contains("int age = 1"));
        assertTrue(testCode.contains("boolean active = true"));
        assertTrue(testCode.contains("double salary = 1.0"));
    }
}
