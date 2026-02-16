package com.utagent.testdata;

import com.utagent.model.ClassInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SmartMockBuilder Tests")
class SmartMockBuilderTest {

    private SmartMockBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SmartMockBuilder();
    }

    @Nested
    @DisplayName("Mock Configuration Generation")
    class MockConfigurationGeneration {

        @Test
        @DisplayName("Should generate mock configuration for interface")
        void shouldGenerateMockConfigurationForInterface() {
            MockConfiguration config = builder.buildMockConfig(RepositoryInterface.class);
            
            assertThat(config).isNotNull();
            assertThat(config.getMockType()).isEqualTo(RepositoryInterface.class);
            assertThat(config.getMockName()).isEqualTo("repositoryInterface");
        }

        @Test
        @DisplayName("Should generate mock configuration for abstract class")
        void shouldGenerateMockConfigurationForAbstractClass() {
            MockConfiguration config = builder.buildMockConfig(AbstractService.class);
            
            assertThat(config).isNotNull();
            assertThat(config.getMockType()).isEqualTo(AbstractService.class);
        }

        @Test
        @DisplayName("Should identify methods to mock")
        void shouldIdentifyMethodsToMock() {
            MockConfiguration config = builder.buildMockConfig(RepositoryInterface.class);
            
            assertThat(config.getMethodsToMock()).isNotEmpty();
            assertThat(config.getMethodsToMock()).contains("findById", "save", "delete");
        }
    }

    @Nested
    @DisplayName("Mock Code Generation")
    class MockCodeGeneration {

        @Test
        @DisplayName("Should generate Mockito import statements")
        void shouldGenerateMockitoImportStatements() {
            MockConfiguration config = builder.buildMockConfig(RepositoryInterface.class);
            String imports = builder.generateImports(config);
            
            assertThat(imports).contains("import org.mockito.Mock;");
            assertThat(imports).contains("import org.mockito.Mockito;");
        }

        @Test
        @DisplayName("Should generate mock field declaration")
        void shouldGenerateMockFieldDeclaration() {
            MockConfiguration config = builder.buildMockConfig(RepositoryInterface.class);
            String declaration = builder.generateMockDeclaration(config);
            
            assertThat(declaration).contains("@Mock");
            assertThat(declaration).contains("RepositoryInterface");
            assertThat(declaration).contains("repositoryInterface");
        }

        @Test
        @DisplayName("Should generate when-then stub code")
        void shouldGenerateWhenThenStubCode() {
            MockConfiguration config = builder.buildMockConfig(RepositoryInterface.class);
            config.addStubbing("findById", "1L", "Optional.of(new Entity())");
            
            String stubCode = builder.generateStubCode(config);
            
            assertThat(stubCode).contains("Mockito.when");
            assertThat(stubCode).contains("findById");
            assertThat(stubCode).contains("thenReturn");
        }

        @Test
        @DisplayName("Should generate verify statements")
        void shouldGenerateVerifyStatements() {
            MockConfiguration config = builder.buildMockConfig(RepositoryInterface.class);
            config.addVerification("save", 1);
            
            String verifyCode = builder.generateVerifyCode(config);
            
            assertThat(verifyCode).contains("Mockito.verify");
            assertThat(verifyCode).contains("save");
            assertThat(verifyCode).contains("times(1)");
        }
    }

    @Nested
    @DisplayName("Dependency Injection Detection")
    class DependencyInjectionDetection {

        @Test
        @DisplayName("Should detect dependencies from class info")
        void shouldDetectDependenciesFromClassInfo() {
            List<FieldInfo> fields = new ArrayList<>();
            fields.add(new FieldInfo("repository", "com.utagent.testdata.SmartMockBuilderTest$RepositoryInterface"));
            
            ClassInfo classInfo = new ClassInfo(
                "com.example",
                "Service",
                "com.example.Service",
                new ArrayList<>(),
                fields,
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                new ArrayList<>(),
                false, false, false, new java.util.HashMap<>()
            );
            
            List<MockConfiguration> configs = builder.detectDependencies(classInfo);
            
            assertThat(configs).hasSize(1);
            assertThat(configs.get(0).getMockType()).isEqualTo(RepositoryInterface.class);
        }

        @Test
        @DisplayName("Should skip final classes")
        void shouldSkipFinalClasses() {
            List<FieldInfo> fields = new ArrayList<>();
            fields.add(new FieldInfo("helper", "com.utagent.testdata.SmartMockBuilderTest$FinalHelper"));
            
            ClassInfo classInfo = new ClassInfo(
                "com.example",
                "Service",
                "com.example.Service",
                new ArrayList<>(),
                fields,
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                new ArrayList<>(),
                false, false, false, new java.util.HashMap<>()
            );
            
            List<MockConfiguration> configs = builder.detectDependencies(classInfo);
            
            assertThat(configs).isEmpty();
        }
    }

    @Nested
    @DisplayName("Smart Stubbing")
    class SmartStubbing {

        @Test
        @DisplayName("Should generate smart return values based on return type")
        void shouldGenerateSmartReturnValuesBasedOnReturnType() {
            MockConfiguration config = builder.buildMockConfig(RepositoryInterface.class);
            
            String findAllStub = builder.generateSmartStub(config, "findAll");
            
            assertThat(findAllStub).contains("Collections.emptyList()");
        }

        @Test
        @DisplayName("Should generate void method stubbing")
        void shouldGenerateVoidMethodStubbing() {
            MockConfiguration config = builder.buildMockConfig(RepositoryInterface.class);
            
            String deleteStub = builder.generateSmartStub(config, "delete");
            
            assertThat(deleteStub).contains("doNothing");
        }
    }

    @Nested
    @DisplayName("Spy Configuration")
    class SpyConfiguration {

        @Test
        @DisplayName("Should generate spy instead of mock when appropriate")
        void shouldGenerateSpyInsteadOfMockWhenAppropriate() {
            MockConfiguration config = builder.buildSpyConfig(ConcreteService.class);
            
            assertThat(config.isSpy()).isTrue();
        }

        @Test
        @DisplayName("Should generate spy declaration")
        void shouldGenerateSpyDeclaration() {
            MockConfiguration config = builder.buildSpyConfig(ConcreteService.class);
            String declaration = builder.generateMockDeclaration(config);
            
            assertThat(declaration).contains("@Spy");
        }
    }

    interface RepositoryInterface {
        Object findById(Long id);
        void save(Object entity);
        void delete(Long id);
        List<Object> findAll();
    }

    static abstract class AbstractService {
        abstract void doSomething();
    }

    static final class FinalHelper {
    }

    static class ConcreteService {
        public String getName() {
            return "default";
        }
    }
}
