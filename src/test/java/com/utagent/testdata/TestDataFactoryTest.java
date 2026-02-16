package com.utagent.testdata;

import com.utagent.model.ClassInfo;
import com.utagent.model.MethodInfo;
import com.utagent.model.FieldInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TestDataFactory Tests")
class TestDataFactoryTest {

    private TestDataFactory factory;

    @BeforeEach
    void setUp() {
        factory = new TestDataFactory();
    }

    @Nested
    @DisplayName("Basic Type Generation")
    class BasicTypeGeneration {

        @Test
        @DisplayName("Should generate valid String test data")
        void shouldGenerateValidStringTestData() {
            TestDataValue value = factory.generate(String.class);
            
            assertThat(value).isNotNull();
            assertThat(value.value()).isInstanceOf(String.class);
            assertThat(value.description()).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate valid Integer test data")
        void shouldGenerateValidIntegerTestData() {
            TestDataValue value = factory.generate(Integer.class);
            
            assertThat(value).isNotNull();
            assertThat(value.value()).isInstanceOf(Integer.class);
        }

        @Test
        @DisplayName("Should generate valid Long test data")
        void shouldGenerateValidLongTestData() {
            TestDataValue value = factory.generate(Long.class);
            
            assertThat(value).isNotNull();
            assertThat(value.value()).isInstanceOf(Long.class);
        }

        @Test
        @DisplayName("Should generate valid Double test data")
        void shouldGenerateValidDoubleTestData() {
            TestDataValue value = factory.generate(Double.class);
            
            assertThat(value).isNotNull();
            assertThat(value.value()).isInstanceOf(Double.class);
        }

        @Test
        @DisplayName("Should generate valid Boolean test data")
        void shouldGenerateValidBooleanTestData() {
            TestDataValue value = factory.generate(Boolean.class);
            
            assertThat(value).isNotNull();
            assertThat(value.value()).isInstanceOf(Boolean.class);
        }

        @Test
        @DisplayName("Should generate valid primitive int test data")
        void shouldGenerateValidPrimitiveIntTestData() {
            TestDataValue value = factory.generate(int.class);
            
            assertThat(value).isNotNull();
            assertThat(value.value()).isInstanceOf(Integer.class);
        }
    }

    @Nested
    @DisplayName("Collection Type Generation")
    class CollectionTypeGeneration {

        @Test
        @DisplayName("Should generate valid List test data")
        void shouldGenerateValidListTestData() {
            TestDataValue value = factory.generate(List.class);
            
            assertThat(value).isNotNull();
            assertThat(value.value()).isInstanceOf(List.class);
        }

        @Test
        @DisplayName("Should generate valid Map test data")
        void shouldGenerateValidMapTestData() {
            TestDataValue value = factory.generate(Map.class);
            
            assertThat(value).isNotNull();
            assertThat(value.value()).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("Should generate empty collection when specified")
        void shouldGenerateEmptyCollectionWhenSpecified() {
            TestDataValue value = factory.generateEmpty(List.class);
            
            assertThat(value).isNotNull();
            List<?> list = (List<?>) value.value();
            assertThat(list).isEmpty();
        }
    }

    @Nested
    @DisplayName("Multiple Values Generation")
    class MultipleValuesGeneration {

        @Test
        @DisplayName("Should generate multiple test data values for type")
        void shouldGenerateMultipleTestDataValuesForType() {
            List<TestDataValue> values = factory.generateMultiple(String.class, 5);
            
            assertThat(values).hasSize(5);
            values.forEach(v -> assertThat(v.value()).isInstanceOf(String.class));
        }

        @Test
        @DisplayName("Should generate diverse values for numeric types")
        void shouldGenerateDiverseValuesForNumericTypes() {
            List<TestDataValue> values = factory.generateMultiple(Integer.class, 3);
            
            assertThat(values).hasSize(3);
            List<Integer> intValues = values.stream()
                .map(v -> (Integer) v.value())
                .toList();
            assertThat(intValues).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Should generate typical test scenarios")
        void shouldGenerateTypicalTestScenarios() {
            List<TestDataValue> scenarios = factory.generateScenarios(String.class);
            
            assertThat(scenarios).isNotEmpty();
            assertThat(scenarios.stream().map(TestDataValue::scenario))
                .containsAnyOf(
                    TestDataScenario.NORMAL,
                    TestDataScenario.EMPTY,
                    TestDataScenario.NULL,
                    TestDataScenario.BOUNDARY
                );
        }
    }

    @Nested
    @DisplayName("Custom Type Generation")
    class CustomTypeGeneration {

        @Test
        @DisplayName("Should generate test data for custom class")
        void shouldGenerateTestDataForCustomClass() {
            List<FieldInfo> fields = new ArrayList<>();
            fields.add(new FieldInfo("name", "java.lang.String"));
            fields.add(new FieldInfo("age", "int"));
            
            ClassInfo classInfo = new ClassInfo(
                "java.lang",
                "String",
                "java.lang.String",
                new ArrayList<>(),
                fields,
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                new ArrayList<>(),
                false, false, false, new java.util.HashMap<>()
            );
            
            TestDataValue value = factory.generateForClass(classInfo);
            
            assertThat(value).isNotNull();
        }

        @Test
        @DisplayName("Should register custom generator")
        void shouldRegisterCustomGenerator() {
            factory.registerGenerator(CustomType.class, () -> 
                new TestDataValue(new CustomType(), "Custom generated", TestDataScenario.NORMAL));
            
            TestDataValue value = factory.generate(CustomType.class);
            
            assertThat(value).isNotNull();
            assertThat(value.value()).isInstanceOf(CustomType.class);
        }
    }

    @Nested
    @DisplayName("Null and Edge Cases")
    class NullAndEdgeCases {

        @Test
        @DisplayName("Should generate null value when allowed")
        void shouldGenerateNullValueWhenAllowed() {
            TestDataValue value = factory.generateNull(String.class);
            
            assertThat(value).isNotNull();
            assertThat(value.value()).isNull();
            assertThat(value.scenario()).isEqualTo(TestDataScenario.NULL);
        }

        @Test
        @DisplayName("Should generate boundary values")
        void shouldGenerateBoundaryValues() {
            List<TestDataValue> boundaries = factory.generateBoundaries(Integer.class);
            
            assertThat(boundaries).isNotEmpty();
            List<Integer> values = boundaries.stream()
                .map(v -> (Integer) v.value())
                .toList();
            assertThat(values).contains(Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        }

        @Test
        @DisplayName("Should handle unknown type gracefully")
        void shouldHandleUnknownTypeGracefully() {
            TestDataValue value = factory.generate(UnknownType.class);
            
            assertThat(value).isNotNull();
        }
    }

    @Nested
    @DisplayName("Method Parameter Generation")
    class MethodParameterGeneration {

        @Test
        @DisplayName("Should generate test data for method parameters")
        void shouldGenerateTestDataForMethodParameters() {
            MethodInfo method = new MethodInfo(
                "calculate",
                "int",
                List.of(
                    new com.utagent.model.ParameterInfo("a", "int", false),
                    new com.utagent.model.ParameterInfo("b", "int", false)
                ),
                new ArrayList<>(),
                null, 0, 0, new ArrayList<>(),
                false, false, false, true, false, false
            );
            
            List<TestDataValue> params = factory.generateForParameters(method);
            
            assertThat(params).hasSize(2);
        }

        @Test
        @DisplayName("Should generate multiple parameter combinations")
        void shouldGenerateMultipleParameterCombinations() {
            MethodInfo method = new MethodInfo(
                "concat",
                "String",
                List.of(
                    new com.utagent.model.ParameterInfo("a", "java.lang.String", false),
                    new com.utagent.model.ParameterInfo("b", "java.lang.String", false)
                ),
                new ArrayList<>(),
                null, 0, 0, new ArrayList<>(),
                false, false, false, true, false, false
            );
            
            List<List<TestDataValue>> combinations = factory.generateParameterCombinations(method, 3);
            
            assertThat(combinations).isNotEmpty();
            combinations.forEach(combo -> assertThat(combo).hasSize(2));
        }
    }

    @Nested
    @DisplayName("Configuration")
    class Configuration {

        @Test
        @DisplayName("Should respect seed for reproducible generation")
        void shouldRespectSeedForReproducibleGeneration() {
            TestDataFactory factory1 = new TestDataFactory(12345L);
            TestDataFactory factory2 = new TestDataFactory(12345L);
            
            TestDataValue value1 = factory1.generate(String.class);
            TestDataValue value2 = factory2.generate(String.class);
            
            assertThat(value1.value()).isEqualTo(value2.value());
        }

        @Test
        @DisplayName("Should configure null probability")
        void shouldConfigureNullProbability() {
            factory.setNullProbability(0.5);
            
            List<TestDataValue> values = factory.generateMultiple(String.class, 100);
            long nullCount = values.stream().map(TestDataValue::value).filter(v -> v == null).count();
            
            assertThat(nullCount).isGreaterThan(0);
        }
    }

    static class CustomType {
    }

    static class UnknownType {
    }
}
