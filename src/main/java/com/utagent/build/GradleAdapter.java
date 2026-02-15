package com.utagent.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GradleAdapter implements BuildToolAdapter {

    @Override
    public String name() {
        return "gradle";
    }

    @Override
    public boolean detect(File projectRoot) {
        return new File(projectRoot, "build.gradle").exists() &&
               !new File(projectRoot, "build.gradle.kts").exists();
    }

    @Override
    public String getTestCommand() {
        return isWindows() ? "gradlew.bat test --quiet" : "./gradlew test --quiet";
    }

    @Override
    public String getCoverageCommand() {
        return isWindows() ? "gradlew.bat test jacocoTestReport --quiet" : "./gradlew test jacocoTestReport --quiet";
    }

    @Override
    public String getCompileCommand() {
        return isWindows() ? "gradlew.bat compileJava --quiet" : "./gradlew compileJava --quiet";
    }

    @Override
    public File getClassesDirectory(File projectRoot) {
        return new File(projectRoot, "build/classes/java/main");
    }

    @Override
    public File getTestClassesDirectory(File projectRoot) {
        return new File(projectRoot, "build/classes/java/test");
    }

    @Override
    public File getCoverageReportFile(File projectRoot) {
        return new File(projectRoot, "build/reports/jacoco/test/jacocoTestReport.xml");
    }

    @Override
    public File getCoverageExecFile(File projectRoot) {
        return new File(projectRoot, "build/jacoco/test.exec");
    }

    @Override
    public File getSourceDirectory(File projectRoot) {
        return new File(projectRoot, "src/main/java");
    }

    @Override
    public File getTestSourceDirectory(File projectRoot) {
        return new File(projectRoot, "src/test/java");
    }

    @Override
    public boolean isMultiModule(File projectRoot) {
        File settingsGradle = new File(projectRoot, "settings.gradle");
        if (!settingsGradle.exists()) {
            settingsGradle = new File(projectRoot, "settings.gradle.kts");
        }
        
        if (!settingsGradle.exists()) {
            return false;
        }
        
        File[] subDirs = projectRoot.listFiles(File::isDirectory);
        if (subDirs == null) {
            return false;
        }
        
        for (File subDir : subDirs) {
            if (new File(subDir, "build.gradle").exists() || 
                new File(subDir, "build.gradle.kts").exists()) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public List<File> getModules(File projectRoot) {
        List<File> modules = new ArrayList<>();
        
        if (!isMultiModule(projectRoot)) {
            modules.add(projectRoot);
            return modules;
        }
        
        File[] subDirs = projectRoot.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                if (new File(subDir, "build.gradle").exists() || 
                    new File(subDir, "build.gradle.kts").exists()) {
                    modules.add(subDir);
                }
            }
        }
        
        return modules;
    }
    
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}
