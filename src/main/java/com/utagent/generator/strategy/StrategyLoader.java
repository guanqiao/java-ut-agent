package com.utagent.generator.strategy;

import com.utagent.parser.FrameworkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 策略加载器，使用 Java SPI 机制加载 TestGenerationStrategy 实现。
 * 支持自动发现和注册策略。
 */
public class StrategyLoader {

    private static final Logger logger = LoggerFactory.getLogger(StrategyLoader.class);

    private final Map<FrameworkType, TestGenerationStrategy> strategies;
    private final boolean useSpi;

    /**
     * 创建策略加载器，默认使用 SPI
     */
    public StrategyLoader() {
        this(true);
    }

    /**
     * 创建策略加载器
     *
     * @param useSpi 是否使用 SPI 加载策略
     */
    public StrategyLoader(boolean useSpi) {
        this.useSpi = useSpi;
        this.strategies = new EnumMap<>(FrameworkType.class);

        if (useSpi) {
            loadStrategiesFromSpi();
        }

        // 如果 SPI 没有加载到策略，使用默认策略
        if (strategies.isEmpty()) {
            loadDefaultStrategies();
        }
    }

    /**
     * 从 SPI 加载策略
     */
    private void loadStrategiesFromSpi() {
        logger.debug("Loading strategies from SPI...");

        ServiceLoader<TestGenerationStrategy> loader = ServiceLoader.load(TestGenerationStrategy.class);
        int count = 0;

        for (TestGenerationStrategy strategy : loader) {
            Set<FrameworkType> supportedFrameworks = strategy.getSupportedFrameworks();

            for (FrameworkType framework : supportedFrameworks) {
                if (strategies.containsKey(framework)) {
                    logger.warn("Strategy for {} already registered, skipping {}",
                        framework, strategy.getClass().getName());
                } else {
                    strategies.put(framework, strategy);
                    logger.debug("Registered strategy {} for framework {}",
                        strategy.getClass().getSimpleName(), framework);
                    count++;
                }
            }
        }

        logger.info("Loaded {} strategies from SPI", count);
    }

    /**
     * 加载默认策略
     */
    private void loadDefaultStrategies() {
        logger.debug("Loading default strategies...");

        registerStrategy(new SpringBootTestStrategy());
        registerStrategy(new SpringMvcTestStrategy());
        registerStrategy(new MyBatisTestStrategy());
        registerStrategy(new MyBatisPlusTestStrategy());

        logger.info("Loaded {} default strategies", strategies.size());
    }

    /**
     * 注册策略
     */
    private void registerStrategy(TestGenerationStrategy strategy) {
        Set<FrameworkType> supportedFrameworks = strategy.getSupportedFrameworks();

        for (FrameworkType framework : supportedFrameworks) {
            if (!strategies.containsKey(framework)) {
                strategies.put(framework, strategy);
            }
        }
    }

    /**
     * 获取指定框架类型的策略
     *
     * @param frameworkType 框架类型
     * @return 策略实现，如果不存在则返回默认策略
     */
    public TestGenerationStrategy getStrategy(FrameworkType frameworkType) {
        return strategies.getOrDefault(frameworkType, getDefaultStrategy());
    }

    /**
     * 获取默认策略
     */
    public TestGenerationStrategy getDefaultStrategy() {
        // 优先返回 Spring MVC 策略作为默认策略
        return strategies.getOrDefault(FrameworkType.SPRING_MVC,
            strategies.values().stream().findFirst().orElse(null));
    }

    /**
     * 获取所有已注册的策略
     */
    public Map<FrameworkType, TestGenerationStrategy> getAllStrategies() {
        return Collections.unmodifiableMap(strategies);
    }

    /**
     * 获取支持的框架类型
     */
    public Set<FrameworkType> getSupportedFrameworks() {
        return Collections.unmodifiableSet(strategies.keySet());
    }

    /**
     * 检查是否支持指定框架
     */
    public boolean supportsFramework(FrameworkType frameworkType) {
        return strategies.containsKey(frameworkType);
    }

    /**
     * 手动注册策略（用于测试或扩展）
     *
     * @param frameworkType 框架类型
     * @param strategy      策略实现
     */
    public void registerStrategy(FrameworkType frameworkType, TestGenerationStrategy strategy) {
        strategies.put(frameworkType, strategy);
        logger.debug("Manually registered strategy {} for framework {}",
            strategy.getClass().getSimpleName(), frameworkType);
    }

    /**
     * 获取已注册策略数量
     */
    public int getStrategyCount() {
        return strategies.size();
    }

    /**
     * 清除所有策略
     */
    public void clear() {
        strategies.clear();
        logger.debug("All strategies cleared");
    }
}
