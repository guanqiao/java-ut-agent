package com.utagent.plugin;

public class EditorContext {
    private final String fileName;
    private final String filePath;
    private final String packageName;
    private final String className;
    private int cursorLine;
    private int cursorColumn;
    private String selectedText;

    public EditorContext(String fileName, String filePath, String packageName, String className) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.packageName = packageName;
        this.className = className;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public int getCursorLine() {
        return cursorLine;
    }

    public void setCursorLine(int cursorLine) {
        this.cursorLine = cursorLine;
    }

    public int getCursorColumn() {
        return cursorColumn;
    }

    public void setCursorColumn(int cursorColumn) {
        this.cursorColumn = cursorColumn;
    }

    public String getSelectedText() {
        return selectedText;
    }

    public void setSelectedText(String selectedText) {
        this.selectedText = selectedText;
    }

    public String getTestFilePath() {
        return filePath
            .replace("src/main/java", "src/test/java")
            .replace(".java", "Test.java");
    }
}
