package com.utagent.maintenance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ImpactAnalysisResult {
    private final String sourceFile;
    private final Set<String> changedMethods;
    private final Set<String> addedMethods;
    private final Set<String> deletedMethods;
    private final Set<String> affectedTests;
    private final ImpactLevel impactLevel;
    private final boolean hasSignatureChange;

    private ImpactAnalysisResult(Builder builder) {
        this.sourceFile = builder.sourceFile;
        this.changedMethods = Collections.unmodifiableSet(new HashSet<>(builder.changedMethods));
        this.addedMethods = Collections.unmodifiableSet(new HashSet<>(builder.addedMethods));
        this.deletedMethods = Collections.unmodifiableSet(new HashSet<>(builder.deletedMethods));
        this.affectedTests = Collections.unmodifiableSet(new HashSet<>(builder.affectedTests));
        this.impactLevel = builder.impactLevel;
        this.hasSignatureChange = builder.hasSignatureChange;
    }

    public String sourceFile() { return sourceFile; }
    public Set<String> changedMethods() { return changedMethods; }
    public Set<String> addedMethods() { return addedMethods; }
    public Set<String> deletedMethods() { return deletedMethods; }
    public Set<String> affectedTests() { return affectedTests; }
    public ImpactLevel impactLevel() { return impactLevel; }
    public boolean hasSignatureChange() { return hasSignatureChange; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String sourceFile;
        private Set<String> changedMethods = new HashSet<>();
        private Set<String> addedMethods = new HashSet<>();
        private Set<String> deletedMethods = new HashSet<>();
        private Set<String> affectedTests = new HashSet<>();
        private ImpactLevel impactLevel = ImpactLevel.NONE;
        private boolean hasSignatureChange = false;

        public Builder sourceFile(String sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        public Builder changedMethod(String method) {
            this.changedMethods.add(method);
            return this;
        }

        public Builder changedMethods(Set<String> methods) {
            this.changedMethods = new HashSet<>(methods);
            return this;
        }

        public Builder addedMethod(String method) {
            this.addedMethods.add(method);
            return this;
        }

        public Builder addedMethods(Set<String> methods) {
            this.addedMethods = new HashSet<>(methods);
            return this;
        }

        public Builder deletedMethod(String method) {
            this.deletedMethods.add(method);
            return this;
        }

        public Builder deletedMethods(Set<String> methods) {
            this.deletedMethods = new HashSet<>(methods);
            return this;
        }

        public Builder affectedTest(String test) {
            this.affectedTests.add(test);
            return this;
        }

        public Builder affectedTests(Set<String> tests) {
            this.affectedTests = new HashSet<>(tests);
            return this;
        }

        public Builder impactLevel(ImpactLevel level) {
            this.impactLevel = level;
            return this;
        }

        public Builder hasSignatureChange(boolean hasChange) {
            this.hasSignatureChange = hasChange;
            return this;
        }

        public ImpactAnalysisResult build() {
            return new ImpactAnalysisResult(this);
        }
    }
}
