package com.utagent.context;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectContext {

    private final File projectRoot;
    private final List<File> sourceDirectories;
    private final List<File> testDirectories;
    private final Set<String> dependencies;
    private final Set<String> testingFrameworks;
    private final boolean hasSpringBoot;
    private final boolean hasMyBatis;
    private final DependencyGraph dependencyGraph;
    private final Map<String, Set<String>> classRelationships;
    private final TestPatterns testPatterns;
    private final NamingConvention namingConvention;

    public ProjectContext(Builder builder) {
        this.projectRoot = builder.projectRoot;
        this.sourceDirectories = builder.sourceDirectories;
        this.testDirectories = builder.testDirectories;
        this.dependencies = builder.dependencies;
        this.testingFrameworks = builder.testingFrameworks;
        this.hasSpringBoot = builder.hasSpringBoot;
        this.hasMyBatis = builder.hasMyBatis;
        this.dependencyGraph = builder.dependencyGraph;
        this.classRelationships = builder.classRelationships;
        this.testPatterns = builder.testPatterns;
        this.namingConvention = builder.namingConvention;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public List<File> getSourceDirectories() {
        return sourceDirectories != null ? sourceDirectories : Collections.emptyList();
    }

    public List<File> getTestDirectories() {
        return testDirectories != null ? testDirectories : Collections.emptyList();
    }

    public Set<String> getDependencies() {
        return dependencies != null ? dependencies : Collections.emptySet();
    }

    public Set<String> getTestingFrameworks() {
        return testingFrameworks != null ? testingFrameworks : Collections.emptySet();
    }

    public boolean hasSpringBoot() {
        return hasSpringBoot;
    }

    public boolean hasMyBatis() {
        return hasMyBatis;
    }

    public DependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }

    public Set<String> getRelatedClasses(String className) {
        if (classRelationships == null) {
            return Collections.emptySet();
        }
        return classRelationships.getOrDefault(className, Collections.emptySet());
    }

    public TestPatterns getTestPatterns() {
        return testPatterns;
    }

    public NamingConvention getNamingConvention() {
        return namingConvention;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private File projectRoot;
        private List<File> sourceDirectories;
        private List<File> testDirectories;
        private Set<String> dependencies;
        private Set<String> testingFrameworks;
        private boolean hasSpringBoot;
        private boolean hasMyBatis;
        private DependencyGraph dependencyGraph;
        private Map<String, Set<String>> classRelationships;
        private TestPatterns testPatterns;
        private NamingConvention namingConvention;

        public Builder projectRoot(File projectRoot) {
            this.projectRoot = projectRoot;
            return this;
        }

        public Builder sourceDirectories(List<File> sourceDirectories) {
            this.sourceDirectories = sourceDirectories;
            return this;
        }

        public Builder testDirectories(List<File> testDirectories) {
            this.testDirectories = testDirectories;
            return this;
        }

        public Builder dependencies(Set<String> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public Builder testingFrameworks(Set<String> testingFrameworks) {
            this.testingFrameworks = testingFrameworks;
            return this;
        }

        public Builder hasSpringBoot(boolean hasSpringBoot) {
            this.hasSpringBoot = hasSpringBoot;
            return this;
        }

        public Builder hasMyBatis(boolean hasMyBatis) {
            this.hasMyBatis = hasMyBatis;
            return this;
        }

        public Builder dependencyGraph(DependencyGraph dependencyGraph) {
            this.dependencyGraph = dependencyGraph;
            return this;
        }

        public Builder classRelationships(Map<String, Set<String>> classRelationships) {
            this.classRelationships = classRelationships;
            return this;
        }

        public Builder testPatterns(TestPatterns testPatterns) {
            this.testPatterns = testPatterns;
            return this;
        }

        public Builder namingConvention(NamingConvention namingConvention) {
            this.namingConvention = namingConvention;
            return this;
        }

        public ProjectContext build() {
            return new ProjectContext(this);
        }
    }
}
