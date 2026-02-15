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

@DisplayName("MyBatisPlusTestStrategy Tests")
class MyBatisPlusTestStrategyTest {

    private MyBatisPlusTestStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MyBatisPlusTestStrategy();
    }

    @Test
    @DisplayName("Should generate test class with MyBatis Plus imports")
    void shouldGenerateTestClassWithMyBatisPlusImports() {
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
        assertTrue(testCode.contains("QueryWrapper"));
        assertTrue(testCode.contains("UpdateWrapper"));
        assertTrue(testCode.contains("Page"));
        assertTrue(testCode.contains("UserMapperTest"));
    }

    @Test
    @DisplayName("Should generate base mapper tests")
    void shouldGenerateBaseMapperTests() {
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

        assertTrue(testCode.contains("shouldSelectByIdSuccessfully"));
        assertTrue(testCode.contains("shouldSelectListWithWrapperSuccessfully"));
        assertTrue(testCode.contains("shouldSelectPageSuccessfully"));
        assertTrue(testCode.contains("shouldInsertSuccessfully"));
        assertTrue(testCode.contains("shouldUpdateByWrapperSuccessfully"));
        assertTrue(testCode.contains("shouldDeleteByIdSuccessfully"));
    }

    @Test
    @DisplayName("Should generate test with QueryWrapper usage")
    void shouldGenerateTestWithQueryWrapperUsage() {
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

        assertTrue(testCode.contains("QueryWrapper<User>"));
        assertTrue(testCode.contains("wrapper.eq"));
    }

    @Test
    @DisplayName("Should generate test with UpdateWrapper usage")
    void shouldGenerateTestWithUpdateWrapperUsage() {
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

        assertTrue(testCode.contains("UpdateWrapper<User>"));
        assertTrue(testCode.contains("wrapper.eq(\"id\", 1L).set(\"status\", 0)"));
    }

    @Test
    @DisplayName("Should generate test with Page usage")
    void shouldGenerateTestWithPageUsage() {
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

        assertTrue(testCode.contains("Page<User>"));
        assertTrue(testCode.contains("new Page<>(1, 10)"));
        assertTrue(testCode.contains("selectPage(page, wrapper)"));
    }

    @Test
    @DisplayName("Should generate pagination tests in additional tests")
    void shouldGeneratePaginationTestsInAdditionalTests() {
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
        String additionalTests = strategy.generateAdditionalTests(classInfo, coverageInfo);

        assertTrue(additionalTests.contains("shouldHandlePaginationWithLargePageNumber"));
        assertTrue(additionalTests.contains("new Page<>(1000, 10)"));
    }

    @Test
    @DisplayName("Should generate condition tests in additional tests")
    void shouldGenerateConditionTestsInAdditionalTests() {
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
        String additionalTests = strategy.generateAdditionalTests(classInfo, coverageInfo);

        assertTrue(additionalTests.contains("shouldHandleComplexQueryConditions"));
        assertTrue(additionalTests.contains("wrapper.and(w -> w.eq"));
        assertTrue(additionalTests.contains("orderByDesc"));
    }

    @Test
    @DisplayName("Should include required imports")
    void shouldIncludeRequiredImports() {
        String[] imports = strategy.getRequiredImports();

        assertNotNull(imports);
        assertTrue(imports.length > 0);
        assertTrue(List.of(imports).contains("org.junit.jupiter.api.Test"));
        assertTrue(List.of(imports).contains("com.baomidou.mybatisplus.core.conditions.query.QueryWrapper"));
        assertTrue(List.of(imports).contains("com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper"));
        assertTrue(List.of(imports).contains("com.baomidou.mybatisplus.extension.plugins.pagination.Page"));
    }

    @Test
    @DisplayName("Should return MyBatis Plus as supported framework")
    void shouldReturnMyBatisPlusAsSupportedFramework() {
        Set<FrameworkType> frameworks = strategy.getSupportedFrameworks();

        assertTrue(frameworks.contains(FrameworkType.MYBATIS_PLUS));
    }

    @Test
    @DisplayName("Should return @Test annotation")
    void shouldReturnTestAnnotation() {
        String annotation = strategy.getTestAnnotation();

        assertEquals("@Test", annotation);
    }

    @Test
    @DisplayName("Should generate test method for specific method")
    void shouldGenerateTestMethodForSpecificMethod() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("customQuery", "List<User>",
            List.of(new ParameterInfo("name", "String")),
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

        String testCode = strategy.generateTestMethod(classInfo, "customQuery", List.of());

        assertNotNull(testCode);
        assertTrue(testCode.contains("shouldCustomQuerySuccessfully"));
    }

    @Test
    @DisplayName("Should generate additional tests with coverage info")
    void shouldGenerateAdditionalTestsWithCoverageInfo() {
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
        assertTrue(additionalTests.contains("pagination"));
        assertTrue(additionalTests.contains("complex query"));
    }

    @Test
    @DisplayName("Should handle Mapper naming convention")
    void shouldHandleMapperNamingConvention() {
        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "OrderMapper",
            "com.example.mapper.OrderMapper",
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("Order"));
        assertFalse(testCode.contains("MapperOrder"));
    }

    @Test
    @DisplayName("Should handle Dao naming convention")
    void shouldHandleDaoNamingConvention() {
        ClassInfo classInfo = new ClassInfo(
            "com.example.dao",
            "UserDao",
            "com.example.dao.UserDao",
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);

        assertTrue(testCode.contains("User"));
        assertFalse(testCode.contains("DaoUser"));
    }

    @Test
    @DisplayName("Should generate Autowired field")
    void shouldGenerateAutowiredField() {
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

        assertTrue(testCode.contains("@Autowired"));
        assertTrue(testCode.contains("private UserMapper userMapper"));
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
}
