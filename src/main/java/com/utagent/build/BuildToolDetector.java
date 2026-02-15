package com.utagent.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BuildToolDetector {

    private static final BuildToolAdapter[] ADAPTERS = {
        new MavenAdapter(),
        new GradleAdapter(),
        new GradleKotlinAdapter()
    };
    
    private BuildToolDetector() {
    }
    
    public static Optional<BuildToolAdapter> detect(File projectRoot) {
        if (projectRoot == null || !projectRoot.isDirectory()) {
            return Optional.empty();
        }
        
        for (BuildToolAdapter adapter : ADAPTERS) {
            if (adapter.detect(projectRoot)) {
                return Optional.of(adapter);
            }
        }
        
        return Optional.empty();
    }
    
    public static BuildToolType detectType(File projectRoot) {
        return detect(projectRoot)
            .map(adapter -> BuildToolType.fromId(adapter.name()))
            .orElse(BuildToolType.UNKNOWN);
    }
    
    public static BuildToolAdapter getAdapter(BuildToolType type) {
        return switch (type) {
            case MAVEN -> new MavenAdapter();
            case GRADLE -> new GradleAdapter();
            case GRADLE_KOTLIN -> new GradleKotlinAdapter();
            case UNKNOWN -> new MavenAdapter();
        };
    }
    
    public static List<File> findProjectRoots(File startDir) {
        List<File> roots = new ArrayList<>();
        findProjectRootsRecursive(startDir, roots);
        return roots;
    }
    
    private static void findProjectRootsRecursive(File dir, List<File> roots) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        
        if (isProjectRoot(dir)) {
            roots.add(dir);
            return;
        }
        
        File[] children = dir.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                findProjectRootsRecursive(child, roots);
            }
        }
    }
    
    private static boolean isProjectRoot(File dir) {
        return new File(dir, "pom.xml").exists() ||
               new File(dir, "build.gradle").exists() ||
               new File(dir, "build.gradle.kts").exists();
    }
}
