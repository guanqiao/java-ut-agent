package com.utagent.plugin;

public class NavigationTarget {
    private final String filePath;
    private final int lineNumber;
    private final String className;
    private final String methodName;

    public NavigationTarget(String filePath, int lineNumber, String className, String methodName) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.className = className;
        this.methodName = methodName;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }
}
