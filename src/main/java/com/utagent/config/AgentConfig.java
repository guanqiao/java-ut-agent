package com.utagent.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.utagent.llm.LLMConfig;

public class AgentConfig {
    
    @JsonProperty("llm")
    private LLMConfig llm;
    
    @JsonProperty("coverage")
    private CoverageConfig coverage;
    
    @JsonProperty("generation")
    private GenerationConfig generation;
    
    @JsonProperty("output")
    private OutputConfig output;
    
    public AgentConfig() {
        this.llm = LLMConfig.defaults();
        this.coverage = CoverageConfig.defaults();
        this.generation = GenerationConfig.defaults();
        this.output = OutputConfig.defaults();
    }
    
    public LLMConfig getLlm() {
        return llm != null ? llm : LLMConfig.defaults();
    }
    
    public CoverageConfig getCoverage() {
        return coverage != null ? coverage : CoverageConfig.defaults();
    }
    
    public GenerationConfig getGeneration() {
        return generation != null ? generation : GenerationConfig.defaults();
    }
    
    public OutputConfig getOutput() {
        return output != null ? output : OutputConfig.defaults();
    }
    
    public void setLlm(LLMConfig llm) {
        this.llm = llm;
    }
    
    public void setCoverage(CoverageConfig coverage) {
        this.coverage = coverage;
    }
    
    public void setGeneration(GenerationConfig generation) {
        this.generation = generation;
    }
    
    public void setOutput(OutputConfig output) {
        this.output = output;
    }
    
    public AgentConfig merge(AgentConfig other) {
        if (other == null) return this;
        
        AgentConfig merged = new AgentConfig();
        
        merged.llm = mergeLLMConfig(this.llm, other.llm);
        merged.coverage = mergeCoverageConfig(this.coverage, other.coverage);
        merged.generation = mergeGenerationConfig(this.generation, other.generation);
        merged.output = mergeOutputConfig(this.output, other.output);
        
        return merged;
    }
    
    private LLMConfig mergeLLMConfig(LLMConfig primary, LLMConfig secondary) {
        if (primary == null) return secondary;
        if (secondary == null) return primary;
        
        return LLMConfig.builder()
            .provider(primary.provider() != null ? primary.provider() : secondary.provider())
            .apiKey(primary.apiKey() != null ? primary.apiKey() : secondary.apiKey())
            .baseUrl(primary.baseUrl() != null ? primary.baseUrl() : secondary.baseUrl())
            .model(primary.model() != null ? primary.model() : secondary.model())
            .temperature(primary.temperature() != null ? primary.temperature() : secondary.temperature())
            .maxTokens(primary.maxTokens() != null ? primary.maxTokens() : secondary.maxTokens())
            .maxRetries(primary.maxRetries() != null ? primary.maxRetries() : secondary.maxRetries())
            .caCertPath(primary.caCertPath() != null ? primary.caCertPath() : secondary.caCertPath())
            .build();
    }
    
    private CoverageConfig mergeCoverageConfig(CoverageConfig primary, CoverageConfig secondary) {
        if (primary == null) return secondary;
        if (secondary == null) return primary;
        
        return CoverageConfig.builder()
            .target(primary.target() != null ? primary.target() : secondary.target())
            .maxIterations(primary.maxIterations() != null ? primary.maxIterations() : secondary.maxIterations())
            .includeBranchCoverage(primary.includeBranchCoverage() != null ? primary.includeBranchCoverage() : secondary.includeBranchCoverage())
            .build();
    }
    
    private GenerationConfig mergeGenerationConfig(GenerationConfig primary, GenerationConfig secondary) {
        if (primary == null) return secondary;
        if (secondary == null) return primary;
        
        return GenerationConfig.builder()
            .strategy(primary.strategy() != null ? primary.strategy() : secondary.strategy())
            .includeNegativeTests(primary.includeNegativeTests() != null ? primary.includeNegativeTests() : secondary.includeNegativeTests())
            .includeEdgeCases(primary.includeEdgeCases() != null ? primary.includeEdgeCases() : secondary.includeEdgeCases())
            .includeParameterizedTests(primary.includeParameterizedTests() != null ? primary.includeParameterizedTests() : secondary.includeParameterizedTests())
            .testDataStrategy(primary.testDataStrategy() != null ? primary.testDataStrategy() : secondary.testDataStrategy())
            .verifyMocks(primary.verifyMocks() != null ? primary.verifyMocks() : secondary.verifyMocks())
            .build();
    }
    
    private OutputConfig mergeOutputConfig(OutputConfig primary, OutputConfig secondary) {
        if (primary == null) return secondary;
        if (secondary == null) return primary;
        
        return OutputConfig.builder()
            .directory(primary.directory() != null ? primary.directory() : secondary.directory())
            .format(primary.format() != null ? primary.format() : secondary.format())
            .verbose(primary.verbose() != null ? primary.verbose() : secondary.verbose())
            .colorOutput(primary.colorOutput() != null ? primary.colorOutput() : secondary.colorOutput())
            .showProgress(primary.showProgress() != null ? primary.showProgress() : secondary.showProgress())
            .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private LLMConfig llm;
        private CoverageConfig coverage;
        private GenerationConfig generation;
        private OutputConfig output;
        
        public Builder llm(LLMConfig llm) {
            this.llm = llm;
            return this;
        }
        
        public Builder coverage(CoverageConfig coverage) {
            this.coverage = coverage;
            return this;
        }
        
        public Builder generation(GenerationConfig generation) {
            this.generation = generation;
            return this;
        }
        
        public Builder output(OutputConfig output) {
            this.output = output;
            return this;
        }
        
        public AgentConfig build() {
            AgentConfig config = new AgentConfig();
            if (llm != null) config.setLlm(llm);
            if (coverage != null) config.setCoverage(coverage);
            if (generation != null) config.setGeneration(generation);
            if (output != null) config.setOutput(output);
            return config;
        }
    }
}
