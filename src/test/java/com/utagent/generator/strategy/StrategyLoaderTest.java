package com.utagent.generator.strategy;

import com.utagent.parser.FrameworkType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StrategyLoaderTest {

    @Test
    void shouldLoadDefaultStrategiesWhenSpiEmpty() {
        StrategyLoader loader = new StrategyLoader();

        assertTrue(loader.getStrategyCount() > 0);
        assertTrue(loader.supportsFramework(FrameworkType.SPRING_BOOT));
        assertTrue(loader.supportsFramework(FrameworkType.SPRING_MVC));
        assertTrue(loader.supportsFramework(FrameworkType.MYBATIS));
        assertTrue(loader.supportsFramework(FrameworkType.MYBATIS_PLUS));
    }

    @Test
    void shouldLoadDefaultStrategiesWhenSpiDisabled() {
        StrategyLoader loader = new StrategyLoader(false);

        assertEquals(4, loader.getStrategyCount());
    }

    @Test
    void shouldGetStrategyForFramework() {
        StrategyLoader loader = new StrategyLoader(false);

        TestGenerationStrategy strategy = loader.getStrategy(FrameworkType.SPRING_BOOT);

        assertNotNull(strategy);
        assertTrue(strategy instanceof SpringBootTestStrategy);
    }

    @Test
    void shouldGetDefaultStrategy() {
        StrategyLoader loader = new StrategyLoader(false);

        TestGenerationStrategy strategy = loader.getDefaultStrategy();

        assertNotNull(strategy);
    }

    @Test
    void shouldReturnDefaultStrategyForUnknownFramework() {
        StrategyLoader loader = new StrategyLoader(false);

        // Create a loader with only one strategy
        StrategyLoader limitedLoader = new StrategyLoader(false);
        limitedLoader.clear();
        limitedLoader.registerStrategy(FrameworkType.SPRING_MVC, new SpringMvcTestStrategy());

        TestGenerationStrategy strategy = limitedLoader.getStrategy(FrameworkType.SPRING_BOOT);

        // Should return default (SpringMvcTestStrategy) when SPRING_BOOT is not registered
        assertNotNull(strategy);
    }

    @Test
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
    void shouldGetSupportedFrameworks() {
        StrategyLoader loader = new StrategyLoader(false);

        Set<FrameworkType> frameworks = loader.getSupportedFrameworks();

        assertEquals(4, frameworks.size());
        assertTrue(frameworks.contains(FrameworkType.SPRING_BOOT));
    }

    @Test
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
    void shouldClearAllStrategies() {
        StrategyLoader loader = new StrategyLoader(false);

        loader.clear();

        assertEquals(0, loader.getStrategyCount());
        assertTrue(loader.getSupportedFrameworks().isEmpty());
    }
}
