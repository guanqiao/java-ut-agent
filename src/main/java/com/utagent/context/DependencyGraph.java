package com.utagent.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DependencyGraph {

    private final Map<String, Set<String>> dependencies;
    private final Map<String, Set<String>> dependents;

    public DependencyGraph() {
        this.dependencies = new HashMap<>();
        this.dependents = new HashMap<>();
    }

    public void addDependency(String from, String to) {
        dependencies.computeIfAbsent(from, k -> new HashSet<>()).add(to);
        dependents.computeIfAbsent(to, k -> new HashSet<>()).add(from);
    }

    public Set<String> getDependencies(String className) {
        return dependencies.getOrDefault(className, new HashSet<>());
    }

    public Set<String> getDependents(String className) {
        return dependents.getOrDefault(className, new HashSet<>());
    }

    public Set<String> getAllClasses() {
        Set<String> allClasses = new HashSet<>();
        allClasses.addAll(dependencies.keySet());
        allClasses.addAll(dependents.keySet());
        return allClasses;
    }

    public boolean hasDependency(String from, String to) {
        return dependencies.containsKey(from) && dependencies.get(from).contains(to);
    }
}
