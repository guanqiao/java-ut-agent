package com.utagent.config;

import com.utagent.llm.LLMConfig;

public record CoverageConfig(
    Double target,
    Integer maxIterations,
    Boolean includeBranchCoverage
) {
    public static final double DEFAULT_TARGET = 0.8;
    public static final int DEFAULT_MAX_ITERATIONS = 10;
    
    public static CoverageConfig defaults() {
        return new CoverageConfig(DEFAULT_TARGET, DEFAULT_MAX_ITERATIONS, true);
    }
    
    public double getTargetOrDefault() {
        return target != null ? target : DEFAULT_TARGET;
    }
    
    public int getMaxIterationsOrDefault() {
        return maxIterations != null ? maxIterations : DEFAULT_MAX_ITERATIONS;
    }
    
    public boolean getIncludeBranchCoverageOrDefault() {
        return includeBranchCoverage != null ? includeBranchCoverage : true;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Double target;
        private Integer maxIterations;
        private Boolean includeBranchCoverage;
        
        public Builder target(Double target) {
            this.target = target;
            return this;
        }
        
        public Builder maxIterations(Integer maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }
        
        public Builder includeBranchCoverage(Boolean includeBranchCoverage) {
            this.includeBranchCoverage = includeBranchCoverage;
            return this;
        }
        
        public CoverageConfig build() {
            return new CoverageConfig(target, maxIterations, includeBranchCoverage);
        }
    }
}
