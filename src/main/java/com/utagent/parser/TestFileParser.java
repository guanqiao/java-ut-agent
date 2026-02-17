package com.utagent.parser;

import com.utagent.model.ParsedTestFile;
import com.utagent.model.ParsedTestMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestFileParser {

    private static final Logger logger = LoggerFactory.getLogger(TestFileParser.class);

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([\\w.]+)\\s*;");
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?:public\\s+)?(?:abstract\\s+)?(?:final\\s+)?class\\s+(\\w+Test)");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+(?:static\\s+)?([\\w.]+(?:\\.\\*)?)\\s*;");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "((?:@\\w+(?:\\([^)]*\\))?\\s*)*)" +
        "(?:public\\s+|private\\s+|protected\\s+)?" +
        "(?:static\\s+)?" +
        "(?:final\\s+)?" +
        "void\\s+(\\w+)\\s*\\([^)]*\\)\\s*(?:throws\\s+[\\w.,\\s]+)?\\s*\\{"
    );
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("@(\\w+)(?:\\([^)]*\\))?");
    private static final Pattern TEST_ANNOTATION_PATTERN = Pattern.compile("@Test(?:\\([^)]*\\))?");
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("@DisplayName\\(?:\\s*\"([^\"]+)\"\\s*\\)");
    private static final Pattern SETUP_METHOD_PATTERN = Pattern.compile("@(BeforeEach|Before|BeforeClass)");
    
    public Optional<ParsedTestFile> parse(File testFile) {
        if (testFile == null || !testFile.exists()) {
            return Optional.empty();
        }

        try {
            String content = Files.readString(testFile.toPath(), StandardCharsets.UTF_8);
            return Optional.of(parseContent(content));
        } catch (IOException e) {
            logger.error("Failed to read test file: {}", testFile.getAbsolutePath(), e);
            return Optional.empty();
        }
    }

    public ParsedTestFile parseContent(String content) {
        ParsedTestFile.Builder builder = ParsedTestFile.builder();

        String packageName = extractPackage(content);
        builder.packageName(packageName);

        String className = extractClassName(content);
        builder.className(className);

        if (packageName != null && className != null) {
            builder.fullyQualifiedName(packageName + "." + className);
        }

        List<String> imports = extractImports(content);
        builder.imports(imports);

        List<String> classAnnotations = extractClassAnnotations(content);
        builder.classAnnotations(classAnnotations);

        List<ParsedTestMethod> testMethods = extractTestMethods(content);
        builder.testMethods(testMethods);

        Set<String> testedMethods = extractTestedMethods(testMethods);
        builder.testedMethods(testedMethods);

        builder.classBody(content);

        logger.debug("Parsed test file: {} with {} test methods", className, testMethods.size());

        return builder.build();
    }

    private String extractPackage(String content) {
        Matcher matcher = PACKAGE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String extractClassName(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private List<String> extractImports(String content) {
        List<String> imports = new ArrayList<>();
        Matcher matcher = IMPORT_PATTERN.matcher(content);
        while (matcher.find()) {
            imports.add(matcher.group(1));
        }
        return imports;
    }

    private List<String> extractClassAnnotations(String content) {
        List<String> annotations = new ArrayList<>();
        
        int classIndex = findClassDeclarationIndex(content);
        if (classIndex == -1) {
            return annotations;
        }

        String beforeClass = content.substring(0, classIndex);
        Matcher matcher = ANNOTATION_PATTERN.matcher(beforeClass);
        
        int lastImportEnd = findLastImportEnd(content);
        String annotationArea = beforeClass.substring(lastImportEnd);
        
        Matcher annotationMatcher = ANNOTATION_PATTERN.matcher(annotationArea);
        while (annotationMatcher.find()) {
            String fullMatch = annotationMatcher.group();
            int start = annotationMatcher.start();
            int end = findAnnotationEnd(annotationArea, start);
            annotations.add(annotationArea.substring(start, end).trim());
        }

        return annotations;
    }

    private int findClassDeclarationIndex(String content) {
        Matcher matcher = CLASS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.start();
        }
        return -1;
    }

    private int findLastImportEnd(String content) {
        int lastEnd = 0;
        Matcher matcher = IMPORT_PATTERN.matcher(content);
        while (matcher.find()) {
            lastEnd = matcher.end();
        }
        return lastEnd;
    }

    private int findAnnotationEnd(String content, int start) {
        int depth = 0;
        int i = start;
        while (i < content.length()) {
            char c = content.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            } else if (c == '\n' && depth == 0) {
                return i;
            }
            i++;
        }
        return content.length();
    }

    private List<ParsedTestMethod> extractTestMethods(String content) {
        List<ParsedTestMethod> methods = new ArrayList<>();

        Matcher methodMatcher = METHOD_PATTERN.matcher(content);
        while (methodMatcher.find()) {
            String annotationBlock = methodMatcher.group(1);
            String methodName = methodMatcher.group(2);
            int methodStart = methodMatcher.end() - 1;

            List<String> annotations = parseAnnotations(annotationBlock);

            boolean isTest = annotations.stream()
                .anyMatch(a -> a.startsWith("@Test") || a.startsWith("@ParameterizedTest"));
            boolean isSetup = annotations.stream()
                .anyMatch(a -> a.startsWith("@BeforeEach") || a.startsWith("@Before"));

            if (isTest || isSetup) {
                String methodBody = extractMethodBody(content, methodStart);
                String testedMethodName = inferTestedMethodName(methodName, annotations);

                ParsedTestMethod testMethod = ParsedTestMethod.builder()
                    .methodName(methodName)
                    .testedMethodName(testedMethodName)
                    .annotations(annotations)
                    .testCode(methodBody)
                    .build();

                methods.add(testMethod);
            }
        }

        return methods;
    }

    private List<String> parseAnnotations(String annotationBlock) {
        List<String> annotations = new ArrayList<>();
        if (annotationBlock == null || annotationBlock.isBlank()) {
            return annotations;
        }

        Matcher matcher = ANNOTATION_PATTERN.matcher(annotationBlock);
        int lastEnd = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = findAnnotationEnd(annotationBlock, start);
            annotations.add(annotationBlock.substring(start, end).trim());
            lastEnd = end;
        }

        return annotations;
    }

    private String extractMethodBody(String content, int startBrace) {
        int depth = 1;
        int i = startBrace + 1;
        while (i < content.length() && depth > 0) {
            char c = content.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
            i++;
        }
        return content.substring(startBrace, i);
    }

    private String inferTestedMethodName(String testMethodName, List<String> annotations) {
        String displayName = extractDisplayName(annotations);
        if (displayName != null) {
            String inferred = inferFromDisplayName(displayName);
            if (inferred != null) {
                return inferred;
            }
        }

        return inferFromTestMethodName(testMethodName);
    }

    private String extractDisplayName(List<String> annotations) {
        for (String annotation : annotations) {
            if (annotation.startsWith("@DisplayName")) {
                Matcher matcher = Pattern.compile("\"([^\"]+)\"").matcher(annotation);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return null;
    }

    private String inferFromDisplayName(String displayName) {
        String[] patterns = {
            "should (\\w+)",
            "test (\\w+)",
            "when (\\w+)",
            "(\\w+) should",
            "(\\w+) when"
        };

        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(displayName);
            if (matcher.find()) {
                return matcher.group(1).toLowerCase();
            }
        }

        return null;
    }

    private String inferFromTestMethodName(String testMethodName) {
        String name = testMethodName;

        if (name.startsWith("test")) {
            name = name.substring(4);
        } else if (name.startsWith("should")) {
            name = name.substring(6);
        } else if (name.startsWith("when")) {
            name = name.substring(4);
        }

        if (name.endsWith("Successfully")) {
            name = name.substring(0, name.length() - 12);
        } else if (name.endsWith("ThrowsException")) {
            name = name.substring(0, name.length() - 15);
        } else if (name.endsWith("Returns")) {
            name = name.substring(0, name.length() - 7);
        }

        return decamelize(name);
    }

    private String decamelize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(str.charAt(0)));

        for (int i = 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private Set<String> extractTestedMethods(List<ParsedTestMethod> testMethods) {
        Set<String> testedMethods = new HashSet<>();
        for (ParsedTestMethod method : testMethods) {
            if (method.testedMethodName() != null && !method.testedMethodName().isEmpty()) {
                testedMethods.add(method.testedMethodName());
            }
        }
        return testedMethods;
    }

    public boolean isTestFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        return file.getName().endsWith("Test.java") || 
               file.getName().endsWith("Tests.java");
    }

    public Optional<File> findExistingTestFile(File sourceFile, File testSourceDir) {
        if (sourceFile == null || !sourceFile.exists()) {
            return Optional.empty();
        }

        String sourceName = sourceFile.getName();
        if (!sourceName.endsWith(".java") || sourceName.endsWith("Test.java")) {
            return Optional.empty();
        }

        String className = sourceName.substring(0, sourceName.length() - 5);
        String testFileName = className + "Test.java";

        String packagePath = extractPackagePath(sourceFile);

        File testFile = new File(testSourceDir, packagePath + "/" + testFileName);
        if (testFile.exists()) {
            return Optional.of(testFile);
        }

        File testsFile = new File(testSourceDir, packagePath + "/" + className + "Tests.java");
        if (testsFile.exists()) {
            return Optional.of(testsFile);
        }

        return Optional.empty();
    }

    private String extractPackagePath(File sourceFile) {
        try {
            String content = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);
            Matcher matcher = PACKAGE_PATTERN.matcher(content);
            if (matcher.find()) {
                String packageName = matcher.group(1);
                return packageName.replace('.', '/');
            }
        } catch (IOException e) {
            logger.debug("Failed to read source file for package extraction: {}", sourceFile.getAbsolutePath());
        }
        return "";
    }
}
