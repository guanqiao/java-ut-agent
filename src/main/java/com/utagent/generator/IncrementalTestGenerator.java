package com.utagent.generator;

import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.model.MethodInfo;
import com.utagent.model.ParsedTestFile;
import com.utagent.model.ParsedTestMethod;
import com.utagent.parser.FrameworkDetector;
import com.utagent.parser.FrameworkType;
import com.utagent.parser.TestFileParser;
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
import java.util.stream.Collectors;

public class IncrementalTestGenerator {

    private static final Logger logger = LoggerFactory.getLogger(IncrementalTestGenerator.class);

    private final TestGenerator testGenerator;
    private final TestFileParser testFileParser;
    private final FrameworkDetector frameworkDetector;

    public IncrementalTestGenerator(TestGenerator testGenerator) {
        this.testGenerator = testGenerator;
        this.testFileParser = new TestFileParser();
        this.frameworkDetector = new FrameworkDetector();
    }

    public IncrementalTestGenerator(TestGenerator testGenerator, 
                                    TestFileParser testFileParser,
                                    FrameworkDetector frameworkDetector) {
        this.testGenerator = testGenerator;
        this.testFileParser = testFileParser;
        this.frameworkDetector = frameworkDetector;
    }

    public IncrementalGenerationResult generateIncremental(ClassInfo classInfo, 
                                                           File existingTestFile,
                                                           List<CoverageInfo> uncoveredInfo) {
        logger.info("Generating incremental tests for: {}", classInfo.className());

        IncrementalGenerationResult result = new IncrementalGenerationResult();
        result.setClassInfo(classInfo);
        result.setExistingTestFile(existingTestFile);

        if (existingTestFile == null || !existingTestFile.exists()) {
            logger.info("No existing test file, generating new test class");
            String newTestCode = testGenerator.generateTestClass(classInfo);
            result.setGeneratedTestCode(newTestCode);
            result.setGenerationType(GenerationType.NEW);
            return result;
        }

        Optional<ParsedTestFile> parsedOpt = testFileParser.parse(existingTestFile);
        if (parsedOpt.isEmpty()) {
            logger.warn("Failed to parse existing test file, generating new test class");
            String newTestCode = testGenerator.generateTestClass(classInfo);
            result.setGeneratedTestCode(newTestCode);
            result.setGenerationType(GenerationType.NEW);
            return result;
        }

        ParsedTestFile existingTests = parsedOpt.get();
        result.setParsedTestFile(existingTests);

        Set<String> testedMethods = existingTests.testedMethods();
        List<MethodInfo> untestedMethods = findUntestedMethods(classInfo, testedMethods);
        
        logger.info("Found {} tested methods, {} untested methods", 
                   testedMethods.size(), untestedMethods.size());

        if (untestedMethods.isEmpty() && uncoveredInfo.isEmpty()) {
            logger.info("All methods already have tests");
            result.setGenerationType(GenerationType.NONE);
            return result;
        }

        List<CoverageInfo> additionalUncovered = filterAlreadyTested(uncoveredInfo, testedMethods);

        String additionalTests = testGenerator.generateAdditionalTests(classInfo, additionalUncovered);

        if (additionalTests == null || additionalTests.trim().isEmpty()) {
            logger.info("No additional tests generated");
            result.setGenerationType(GenerationType.NONE);
            return result;
        }

        String mergedTestCode = mergeTestCode(existingTests, additionalTests, classInfo);
        result.setGeneratedTestCode(mergedTestCode);
        result.setGenerationType(GenerationType.INCREMENTAL);
        result.setAddedTestMethods(extractMethodNames(additionalTests));

        return result;
    }

    private List<MethodInfo> findUntestedMethods(ClassInfo classInfo, Set<String> testedMethods) {
        return classInfo.methods().stream()
            .filter(m -> !m.isPrivate() && !m.isAbstract())
            .filter(m -> !isMethodTested(m, testedMethods))
            .collect(Collectors.toList());
    }

    private boolean isMethodTested(MethodInfo method, Set<String> testedMethods) {
        String methodName = method.name().toLowerCase();
        
        for (String tested : testedMethods) {
            if (tested != null && tested.toLowerCase().equals(methodName)) {
                return true;
            }
            if (tested != null && tested.toLowerCase().contains(methodName)) {
                return true;
            }
        }
        
        return false;
    }

    private List<CoverageInfo> filterAlreadyTested(List<CoverageInfo> uncoveredInfo, 
                                                   Set<String> testedMethods) {
        if (uncoveredInfo == null || uncoveredInfo.isEmpty()) {
            return new ArrayList<>();
        }

        return uncoveredInfo.stream()
            .filter(info -> {
                String methodName = info.methodName();
                if (methodName == null) return true;
                return !isMethodTestedByName(methodName, testedMethods);
            })
            .collect(Collectors.toList());
    }

    private boolean isMethodTestedByName(String methodName, Set<String> testedMethods) {
        String lowerMethodName = methodName.toLowerCase();
        for (String tested : testedMethods) {
            if (tested != null && tested.toLowerCase().equals(lowerMethodName)) {
                return true;
            }
        }
        return false;
    }

    private String mergeTestCode(ParsedTestFile existingTests, 
                                 String additionalTests, 
                                 ClassInfo classInfo) {
        String existingContent = existingTests.classBody();
        
        int lastBraceIndex = existingContent.lastIndexOf('}');
        if (lastBraceIndex <= 0) {
            logger.warn("Invalid existing test file structure");
            return existingContent;
        }

        String additionalMethods = extractMethodBodies(additionalTests);
        
        if (additionalMethods.trim().isEmpty()) {
            return existingContent;
        }

        StringBuilder merged = new StringBuilder();
        merged.append(existingContent.substring(0, lastBraceIndex));
        merged.append("\n");
        merged.append(additionalMethods);
        merged.append("\n}\n");

        return merged.toString();
    }

    private String extractMethodBodies(String testCode) {
        if (testCode == null || testCode.trim().isEmpty()) {
            return "";
        }

        StringBuilder methods = new StringBuilder();
        
        if (testCode.contains("public class") || testCode.contains("class ")) {
            Pattern classPattern = Pattern.compile("class\\s+\\w+\\s*\\{");
            Matcher classMatcher = classPattern.matcher(testCode);
            
            if (classMatcher.find()) {
                int classStart = classMatcher.end();
                int depth = 1;
                int i = classStart;
                while (i < testCode.length() && depth > 0) {
                    char c = testCode.charAt(i);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    i++;
                }
                return testCode.substring(classStart, i - 1).trim();
            }
        }

        return testCode.trim();
    }

    private List<String> extractMethodNames(String testCode) {
        List<String> methodNames = new ArrayList<>();
        Pattern methodPattern = Pattern.compile(
            "(?:@Test|@ParameterizedTest)[^}]*?void\\s+(\\w+)\\s*\\(",
            Pattern.DOTALL
        );
        
        Matcher matcher = methodPattern.matcher(testCode);
        while (matcher.find()) {
            methodNames.add(matcher.group(1));
        }
        
        return methodNames;
    }

    public boolean hasExistingTests(File testFile) {
        if (testFile == null || !testFile.exists()) {
            return false;
        }
        
        Optional<ParsedTestFile> parsed = testFileParser.parse(testFile);
        return parsed.isPresent() && parsed.get().getTestMethodCount() > 0;
    }

    public Set<String> getTestedMethodNames(File testFile) {
        if (testFile == null || !testFile.exists()) {
            return new HashSet<>();
        }
        
        Optional<ParsedTestFile> parsed = testFileParser.parse(testFile);
        if (parsed.isEmpty()) {
            return new HashSet<>();
        }
        
        return parsed.get().testedMethods();
    }

    public enum GenerationType {
        NEW,
        INCREMENTAL,
        NONE
    }

    public static class IncrementalGenerationResult {
        private ClassInfo classInfo;
        private File existingTestFile;
        private ParsedTestFile parsedTestFile;
        private String generatedTestCode;
        private GenerationType generationType;
        private List<String> addedTestMethods = new ArrayList<>();

        public ClassInfo getClassInfo() {
            return classInfo;
        }

        public void setClassInfo(ClassInfo classInfo) {
            this.classInfo = classInfo;
        }

        public File getExistingTestFile() {
            return existingTestFile;
        }

        public void setExistingTestFile(File existingTestFile) {
            this.existingTestFile = existingTestFile;
        }

        public ParsedTestFile getParsedTestFile() {
            return parsedTestFile;
        }

        public void setParsedTestFile(ParsedTestFile parsedTestFile) {
            this.parsedTestFile = parsedTestFile;
        }

        public String getGeneratedTestCode() {
            return generatedTestCode;
        }

        public void setGeneratedTestCode(String generatedTestCode) {
            this.generatedTestCode = generatedTestCode;
        }

        public GenerationType getGenerationType() {
            return generationType;
        }

        public void setGenerationType(GenerationType generationType) {
            this.generationType = generationType;
        }

        public List<String> getAddedTestMethods() {
            return addedTestMethods;
        }

        public void setAddedTestMethods(List<String> addedTestMethods) {
            this.addedTestMethods = addedTestMethods;
        }

        public boolean hasNewTests() {
            return generationType == GenerationType.NEW || 
                   generationType == GenerationType.INCREMENTAL;
        }

        public int getAddedMethodCount() {
            return addedTestMethods.size();
        }

        public int getExistingMethodCount() {
            return parsedTestFile != null ? parsedTestFile.getTestMethodCount() : 0;
        }
    }
}
