package com.utagent.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProjectContextAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ProjectContextAnalyzer.class);

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+);");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?:public\\s+)?class\\s+(\\w+)");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+([\\w.]+);");
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>");

    private final Map<String, ProjectContext> contextCache = new ConcurrentHashMap<>();

    public ProjectContext analyze(File projectRoot) {
        if (projectRoot == null || !projectRoot.exists()) {
            return ProjectContext.builder().build();
        }

        String cacheKey = projectRoot.getAbsolutePath();
        if (contextCache.containsKey(cacheKey)) {
            return contextCache.get(cacheKey);
        }

        logger.info("Analyzing project: {}", projectRoot.getAbsolutePath());

        ProjectContext context = ProjectContext.builder()
            .projectRoot(projectRoot)
            .sourceDirectories(findSourceDirectories(projectRoot))
            .testDirectories(findTestDirectories(projectRoot))
            .dependencies(detectDependencies(projectRoot))
            .testingFrameworks(detectTestingFrameworks(projectRoot))
            .hasSpringBoot(detectSpringBoot(projectRoot))
            .hasMyBatis(detectMyBatis(projectRoot))
            .dependencyGraph(buildDependencyGraph(projectRoot))
            .classRelationships(buildClassRelationships(projectRoot))
            .testPatterns(detectTestPatterns(projectRoot))
            .namingConvention(detectNamingConvention(projectRoot))
            .build();

        contextCache.put(cacheKey, context);
        return context;
    }

    public void invalidateCache(File projectRoot) {
        if (projectRoot != null) {
            contextCache.remove(projectRoot.getAbsolutePath());
        }
    }

    private List<File> findSourceDirectories(File projectRoot) {
        List<File> directories = new ArrayList<>();
        
        File mainJava = new File(projectRoot, "src/main/java");
        if (mainJava.exists()) {
            directories.add(mainJava);
        }

        findDirectories(projectRoot, "java", directories, true);
        
        return directories;
    }

    private List<File> findTestDirectories(File projectRoot) {
        List<File> directories = new ArrayList<>();
        
        File testJava = new File(projectRoot, "src/test/java");
        if (testJava.exists()) {
            directories.add(testJava);
        }

        findDirectories(projectRoot, "test", directories, false);
        
        return directories;
    }

    private void findDirectories(File root, String name, List<File> result, boolean excludeTest) {
        File[] files = root.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                if (file.getName().equals(name) && (!excludeTest || !file.getPath().contains("test"))) {
                    result.add(file);
                } else {
                    findDirectories(file, name, result, excludeTest);
                }
            }
        }
    }

    private Set<String> detectDependencies(File projectRoot) {
        Set<String> dependencies = new HashSet<>();
        
        File pomXml = new File(projectRoot, "pom.xml");
        if (pomXml.exists()) {
            try {
                String content = Files.readString(pomXml.toPath());
                Matcher matcher = DEPENDENCY_PATTERN.matcher(content);
                while (matcher.find()) {
                    dependencies.add(matcher.group(1) + ":" + matcher.group(2));
                }
            } catch (IOException e) {
                logger.warn("Failed to read pom.xml: {}", e.getMessage());
            }
        }

        File buildGradle = new File(projectRoot, "build.gradle");
        if (buildGradle.exists()) {
            try {
                String content = Files.readString(buildGradle.toPath());
                Pattern gradlePattern = Pattern.compile("implementation\\s*['\"]([^'\"]+)['\"]");
                Matcher matcher = gradlePattern.matcher(content);
                while (matcher.find()) {
                    dependencies.add(matcher.group(1));
                }
            } catch (IOException e) {
                logger.warn("Failed to read build.gradle: {}", e.getMessage());
            }
        }
        
        return dependencies;
    }

    private Set<String> detectTestingFrameworks(File projectRoot) {
        Set<String> frameworks = new HashSet<>();
        Set<String> dependencies = detectDependencies(projectRoot);
        
        if (dependencies.stream().anyMatch(d -> d.contains("junit-jupiter") || d.contains("junit5"))) {
            frameworks.add("JUnit 5");
        }
        if (dependencies.stream().anyMatch(d -> d.contains("junit") && !d.contains("jupiter"))) {
            frameworks.add("JUnit 4");
        }
        if (dependencies.stream().anyMatch(d -> d.contains("mockito"))) {
            frameworks.add("Mockito");
        }
        if (dependencies.stream().anyMatch(d -> d.contains("assertj"))) {
            frameworks.add("AssertJ");
        }
        if (dependencies.stream().anyMatch(d -> d.contains("testng"))) {
            frameworks.add("TestNG");
        }
        
        if (frameworks.isEmpty()) {
            frameworks.add("JUnit 5");
        }
        
        return frameworks;
    }

    private boolean detectSpringBoot(File projectRoot) {
        Set<String> dependencies = detectDependencies(projectRoot);
        return dependencies.stream().anyMatch(d -> 
            d.contains("spring-boot") || d.contains("spring-boot-starter"));
    }

    private boolean detectMyBatis(File projectRoot) {
        Set<String> dependencies = detectDependencies(projectRoot);
        return dependencies.stream().anyMatch(d -> 
            d.contains("mybatis") || d.contains("mybatis-plus"));
    }

    private DependencyGraph buildDependencyGraph(File projectRoot) {
        DependencyGraph graph = new DependencyGraph();
        
        List<File> sourceDirs = findSourceDirectories(projectRoot);
        Map<String, String> classToPackage = new HashMap<>();
        
        for (File sourceDir : sourceDirs) {
            try {
                List<File> javaFiles = findJavaFiles(sourceDir);
                for (File javaFile : javaFiles) {
                    String content = Files.readString(javaFile.toPath());
                    
                    Matcher packageMatcher = PACKAGE_PATTERN.matcher(content);
                    String packageName = packageMatcher.find() ? packageMatcher.group(1) : "";
                    
                    Matcher classMatcher = CLASS_PATTERN.matcher(content);
                    if (classMatcher.find()) {
                        String className = classMatcher.group(1);
                        String fullName = packageName.isEmpty() ? className : packageName + "." + className;
                        classToPackage.put(className, fullName);
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to read source file: {}", e.getMessage());
            }
        }
        
        for (File sourceDir : sourceDirs) {
            try {
                List<File> javaFiles = findJavaFiles(sourceDir);
                for (File javaFile : javaFiles) {
                    String content = Files.readString(javaFile.toPath());
                    
                    Matcher classMatcher = CLASS_PATTERN.matcher(content);
                    String fromClass = classMatcher.find() ? classMatcher.group(1) : null;
                    
                    if (fromClass != null) {
                        Matcher importMatcher = IMPORT_PATTERN.matcher(content);
                        while (importMatcher.find()) {
                            String imported = importMatcher.group(1);
                            String importedClass = imported.substring(imported.lastIndexOf('.') + 1);
                            if (classToPackage.containsKey(importedClass)) {
                                graph.addDependency(fromClass, importedClass);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to analyze dependencies: {}", e.getMessage());
            }
        }
        
        return graph;
    }

    private Map<String, Set<String>> buildClassRelationships(File projectRoot) {
        Map<String, Set<String>> relationships = new HashMap<>();
        DependencyGraph graph = buildDependencyGraph(projectRoot);
        
        for (String className : graph.getAllClasses()) {
            Set<String> related = new HashSet<>();
            related.addAll(graph.getDependencies(className));
            related.addAll(graph.getDependents(className));
            relationships.put(className, related);
        }
        
        return relationships;
    }

    private TestPatterns detectTestPatterns(File projectRoot) {
        Set<String> assertionStyles = new HashSet<>();
        Set<String> mockFrameworks = new HashSet<>();
        boolean usesBeforeEach = false;
        boolean usesParameterizedTests = false;
        boolean usesNestedTests = false;
        
        List<File> testDirs = findTestDirectories(projectRoot);
        for (File testDir : testDirs) {
            try {
                List<File> testFiles = findJavaFiles(testDir);
                for (File testFile : testFiles) {
                    String content = Files.readString(testFile.toPath());
                    
                    if (content.contains("assertThat")) {
                        assertionStyles.add("AssertJ");
                    }
                    if (content.contains("assertEquals") || content.contains("assertTrue")) {
                        assertionStyles.add("JUnit");
                    }
                    if (content.contains("@Mock") || content.contains("Mockito.")) {
                        mockFrameworks.add("Mockito");
                    }
                    if (content.contains("@BeforeEach")) {
                        usesBeforeEach = true;
                    }
                    if (content.contains("@ParameterizedTest")) {
                        usesParameterizedTests = true;
                    }
                    if (content.contains("@Nested")) {
                        usesNestedTests = true;
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to analyze test patterns: {}", e.getMessage());
            }
        }
        
        if (assertionStyles.isEmpty()) {
            assertionStyles.add("AssertJ");
        }
        if (mockFrameworks.isEmpty()) {
            mockFrameworks.add("Mockito");
        }
        
        return new TestPatterns(assertionStyles, mockFrameworks, usesBeforeEach, usesParameterizedTests, usesNestedTests);
    }

    private NamingConvention detectNamingConvention(File projectRoot) {
        String testClassSuffix = "Test";
        String testMethodPrefix = "should";
        boolean useDisplayName = false;
        
        List<File> testDirs = findTestDirectories(projectRoot);
        for (File testDir : testDirs) {
            try {
                List<File> testFiles = findJavaFiles(testDir);
                for (File testFile : testFiles) {
                    String fileName = testFile.getName();
                    if (fileName.endsWith("Tests.java")) {
                        testClassSuffix = "Tests";
                    }
                    
                    String content = Files.readString(testFile.toPath());
                    if (content.contains("@DisplayName")) {
                        useDisplayName = true;
                    }
                    
                    if (content.contains("test") && content.contains("void test")) {
                        testMethodPrefix = "test";
                    }
                }
            } catch (IOException e) {
                logger.warn("Failed to detect naming convention: {}", e.getMessage());
            }
        }
        
        return new NamingConvention(testClassSuffix, testMethodPrefix, useDisplayName);
    }

    private List<File> findJavaFiles(File directory) throws IOException {
        if (!directory.exists()) {
            return new ArrayList<>();
        }
        
        return Files.walk(directory.toPath())
            .filter(p -> p.toString().endsWith(".java"))
            .map(Path::toFile)
            .collect(Collectors.toList());
    }
}
