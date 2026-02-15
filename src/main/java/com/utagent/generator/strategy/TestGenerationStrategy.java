package com.utagent.generator.strategy;

import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.parser.FrameworkType;

import java.util.List;
import java.util.Set;

public interface TestGenerationStrategy {

    String generateTestClass(ClassInfo classInfo);

    String generateTestMethod(ClassInfo classInfo, String methodName, List<String> uncoveredLines);

    String generateAdditionalTests(ClassInfo classInfo, List<CoverageInfo> coverageInfo);

    Set<FrameworkType> getSupportedFrameworks();

    String getTestAnnotation();

    String[] getRequiredImports();

    default String buildImportStatements() {
        StringBuilder sb = new StringBuilder();
        for (String imp : getRequiredImports()) {
            sb.append("import ").append(imp).append(";\n");
        }
        return sb.toString();
    }
}
