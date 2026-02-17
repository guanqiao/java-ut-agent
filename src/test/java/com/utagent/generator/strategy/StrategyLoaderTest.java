package com.utagent.generator.strategy;

import com.utagent.parser.FrameworkType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StrategyLoader Tests")
class StrategyLoaderTest {

    @Nested
    @DisplayName("SPI Loading Tests")
    class SpiLoadingTests {

        @Test
        @DisplayName("Should load strategies from SPI")
        void shouldLoadStrategiesFromSpi() {
            StrategyLoader loader = new StrategyLoader(true);

            assertTrue(loader.getStrategyCount() > 0);
        }

        @Test
        @DisplayName("Should load all strategies from SPI configuration")
        void shouldLoadAllStrategiesFromSpiConfiguration() {
            StrategyLoader loader = new StrategyLoader(true);

            assertTrue(loader.supportsFramework(FrameworkType.SPRING_BOOT));
            assertTrue(loader.supportsFramework(FrameworkType.SPRING_MVC));
            assertTrue(loader.supportsFramework(FrameworkType.MYBATIS));
            assertTrue(loader.supportsFramework(FrameworkType.MYBATIS_PLUS));
            assertTrue(loader.supportsFramework(FrameworkType.DUBBO));
            assertTrue(loader.supportsFramework(FrameworkType.REACTIVE));
        }
    }

    @Nested
    @DisplayName("Default Loading Tests")
    class DefaultLoadingTests {

        @Test
        @DisplayName("Should load default strategies when SPI disabled")
        void shouldLoadDefaultStrategiesWhenSpiDisabled() {
            StrategyLoader loader = new StrategyLoader(false);

            assertEquals(4, loader.getStrategyCount());
        }

        @Test
        @DisplayName("Should load default strategies when SPI empty")
        void shouldLoadDefaultStrategiesWhenSpiEmpty() {
            StrategyLoader loader = new StrategyLoader(false);

            assertTrue(loader.getStrategyCount() > 0);
            assertTrue(loader.supportsFramework(FrameworkType.SPRING_BOOT));
            assertTrue(loader.supportsFramework(FrameworkType.SPRING_MVC));
            assertTrue(loader.supportsFramework(FrameworkType.MYBATIS));
            assertTrue(loader.supportsFramework(FrameworkType.MYBATIS_PLUS));
        }
    }

    @Nested
    @DisplayName("Strategy Retrieval Tests")
    class StrategyRetrievalTests {

        @Test
        @DisplayName("Should get strategy for framework")
        void shouldGetStrategyForFramework() {
            StrategyLoader loader = new StrategyLoader(false);

            TestGenerationStrategy strategy = loader.getStrategy(FrameworkType.SPRING_BOOT);

            assertNotNull(strategy);
            assertTrue(strategy instanceof SpringBootTestStrategy);
        }

        @Test
        @DisplayName("Should get default strategy")
        void shouldGetDefaultStrategy() {
            StrategyLoader loader = new StrategyLoader(false);

            TestGenerationStrategy strategy = loader.getDefaultStrategy();

            assertNotNull(strategy);
        }

        @Test
        @DisplayName("Should return default strategy for unknown framework")
        void shouldReturnDefaultStrategyForUnknownFramework() {
            StrategyLoader limitedLoader = new StrategyLoader(false);
            limitedLoader.clear();
            limitedLoader.registerStrategy(FrameworkType.SPRING_MVC, new SpringMvcTestStrategy());

            TestGenerationStrategy strategy = limitedLoader.getStrategy(FrameworkType.SPRING_BOOT);

            assertNotNull(strategy);
        }

        @Test
        @DisplayName("Should get all strategies")
        void shouldGetAllStrategies() {
            StrategyLoader loader = new StrategyLoader(false);

            var strategies = loader.getAllStrategies();

            assertEquals(4, strategies.size());
            assertTrue(strategies.containsKey(FrameworkType.SPRING_BOOT));
            assertTrue(strategies.containsKey(FrameworkType.SPRING_MVC));
            assertTrue(strategies.containsKey(FrameworkType.MYBATIS));
            assertTrue(strategies.containsKey(FrameworkType.MYBATIS_PLUS));
        }

        @Test
        @DisplayName("Should get supported frameworks")
        void shouldGetSupportedFrameworks() {
            StrategyLoader loader = new StrategyLoader(false);

            Set<FrameworkType> frameworks = loader.getSupportedFrameworks();

            assertEquals(4, frameworks.size());
            assertTrue(frameworks.contains(FrameworkType.SPRING_BOOT));
        }
    }

    @Nested
    @DisplayName("Manual Registration Tests")
    class ManualRegistrationTests {

        @Test
        @DisplayName("Should register strategy manually")
        void shouldRegisterStrategyManually() {
            StrategyLoader loader = new StrategyLoader(false);
            int initialCount = loader.getStrategyCount();

            TestGenerationStrategy customStrategy = new TestGenerationStrategy() {
                @Override
                public String generateTestClass(com.utagent.model.ClassInfo classInfo) {
                    return "";
                }

                @Override
                public String generateTestMethod(com.utagent.model.ClassInfo classInfo, String methodName, java.util.List<String> uncoveredLines) {
                    return "";
                }

                @Override
                public String generateAdditionalTests(com.utagent.model.ClassInfo classInfo, java.util.List<com.utagent.model.CoverageInfo> coverageInfo) {
                    return "";
                }

                @Override
                public Set<FrameworkType> getSupportedFrameworks() {
                    return Set.of(FrameworkType.DUBBO);
                }

                @Override
                public String getTestAnnotation() {
                    return "@Test";
                }

                @Override
                public String[] getRequiredImports() {
                    return new String[0];
                }
            };

            loader.registerStrategy(FrameworkType.DUBBO, customStrategy);

            assertEquals(initialCount + 1, loader.getStrategyCount());
            assertTrue(loader.supportsFramework(FrameworkType.DUBBO));
        }

        @Test
        @DisplayName("Should clear all strategies")
        void shouldClearAllStrategies() {
            StrategyLoader loader = new StrategyLoader(false);

            loader.clear();

            assertEquals(0, loader.getStrategyCount());
            assertTrue(loader.getSupportedFrameworks().isEmpty());
        }
    }
}
