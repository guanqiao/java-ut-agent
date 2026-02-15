package com.utagent.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenAdapter implements BuildToolAdapter {

    @Override
    public String name() {
        return "maven";
    }

    @Override
    public boolean detect(File projectRoot) {
        return new File(projectRoot, "pom.xml").exists();
    }

    @Override
    public String getTestCommand() {
        return "mvn test -q";
    }

    @Override
    public String getCoverageCommand() {
        return "mvn test jacoco:report -q";
    }

    @Override
    public String getCompileCommand() {
        return "mvn compile -q";
    }

    @Override
    public File getClassesDirectory(File projectRoot) {
        return new File(projectRoot, "target/classes");
    }

    @Override
    public File getTestClassesDirectory(File projectRoot) {
        return new File(projectRoot, "target/test-classes");
    }

    @Override
    public File getCoverageReportFile(File projectRoot) {
        return new File(projectRoot, "target/site/jacoco/jacoco.xml");
    }

    @Override
    public File getCoverageExecFile(File projectRoot) {
        return new File(projectRoot, "target/jacoco.exec");
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
        File pomXml = new File(projectRoot, "pom.xml");
        if (!pomXml.exists()) {
            return false;
        }
        
        File[] subDirs = projectRoot.listFiles(File::isDirectory);
        if (subDirs == null) {
            return false;
        }
        
        for (File subDir : subDirs) {
            if (new File(subDir, "pom.xml").exists()) {
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
                if (new File(subDir, "pom.xml").exists()) {
                    modules.add(subDir);
                }
            }
        }
        
        return modules;
    }
}
