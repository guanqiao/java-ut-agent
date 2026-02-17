package com.utagent.ai;

public class RefactoringSuggestion {
    private final String description;
    private final String beforeCode;
    private final String afterCode;
    private final RefactoringType type;

    public RefactoringSuggestion(String description, String beforeCode, String afterCode, RefactoringType type) {
        this.description = description;
        this.beforeCode = beforeCode;
        this.afterCode = afterCode;
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public String getBeforeCode() {
        return beforeCode;
    }

    public String getAfterCode() {
        return afterCode;
    }

    public RefactoringType getType() {
        return type;
    }
}
