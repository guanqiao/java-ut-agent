package com.utagent.config;

public record GenerationConfig(
    String strategy,
    Boolean includeNegativeTests,
    Boolean includeEdgeCases,
    Boolean includeParameterizedTests,
    String testDataStrategy,
    Boolean verifyMocks
) {
    public static final String STRATEGY_AI = "ai";
    public static final String STRATEGY_TEMPLATE = "template";
    public static final String TEST_DATA_INSTANCIO = "instancio";
    public static final String TEST_DATA_BUILDER = "builder";
    public static final String TEST_DATA_SIMPLE = "simple";
    
    public static GenerationConfig defaults() {
        return new GenerationConfig(
            STRATEGY_AI,
            true,
            true,
            false,
            TEST_DATA_SIMPLE,
            true
        );
    }
    
    public String getStrategyOrDefault() {
        return strategy != null ? strategy : STRATEGY_AI;
    }
    
    public boolean getIncludeNegativeTestsOrDefault() {
        return includeNegativeTests != null ? includeNegativeTests : true;
    }
    
    public boolean getIncludeEdgeCasesOrDefault() {
        return includeEdgeCases != null ? includeEdgeCases : true;
    }
    
    public boolean getIncludeParameterizedTestsOrDefault() {
        return includeParameterizedTests != null ? includeParameterizedTests : false;
    }
    
    public String getTestDataStrategyOrDefault() {
        return testDataStrategy != null ? testDataStrategy : TEST_DATA_SIMPLE;
    }
    
    public boolean getVerifyMocksOrDefault() {
        return verifyMocks != null ? verifyMocks : true;
    }
    
    public boolean isAIStrategy() {
        return STRATEGY_AI.equalsIgnoreCase(getStrategyOrDefault());
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String strategy;
        private Boolean includeNegativeTests;
        private Boolean includeEdgeCases;
        private Boolean includeParameterizedTests;
        private String testDataStrategy;
        private Boolean verifyMocks;
        
        public Builder strategy(String strategy) {
            this.strategy = strategy;
            return this;
        }
        
        public Builder includeNegativeTests(Boolean includeNegativeTests) {
            this.includeNegativeTests = includeNegativeTests;
            return this;
        }
        
        public Builder includeEdgeCases(Boolean includeEdgeCases) {
            this.includeEdgeCases = includeEdgeCases;
            return this;
        }
        
        public Builder includeParameterizedTests(Boolean includeParameterizedTests) {
            this.includeParameterizedTests = includeParameterizedTests;
            return this;
        }
        
        public Builder testDataStrategy(String testDataStrategy) {
            this.testDataStrategy = testDataStrategy;
            return this;
        }
        
        public Builder verifyMocks(Boolean verifyMocks) {
            this.verifyMocks = verifyMocks;
            return this;
        }
        
        public GenerationConfig build() {
            return new GenerationConfig(
                strategy, includeNegativeTests, includeEdgeCases,
                includeParameterizedTests, testDataStrategy, verifyMocks
            );
        }
    }
}
