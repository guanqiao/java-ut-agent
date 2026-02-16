package com.utagent.ide;

public class IDEContext {
    private final String packageName;
    private final String className;
    private final String filePath;
    private final String projectRoot;

    public IDEContext(String packageName, String className, String filePath, String projectRoot) {
        this.packageName = packageName;
        this.className = className;
        this.filePath = filePath;
        this.projectRoot = projectRoot;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    public String getTestClassName() {
        return className + "Test";
    }

    public String getTestPackage() {
        return packageName;
    }
}
