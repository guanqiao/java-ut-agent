package com.utagent.plugin;

public class DiffResult {
    private final String oldContent;
    private final String newContent;
    private final int addedLines;
    private final int removedLines;
    private final int modifiedLines;

    public DiffResult(String oldContent, String newContent, int addedLines, 
                     int removedLines, int modifiedLines) {
        this.oldContent = oldContent;
        this.newContent = newContent;
        this.addedLines = addedLines;
        this.removedLines = removedLines;
        this.modifiedLines = modifiedLines;
    }

    public static DiffResult noChange(String content) {
        return new DiffResult(content, content, 0, 0, 0);
    }

    public boolean hasChanges() {
        return !oldContent.equals(newContent);
    }

    public String getOldContent() {
        return oldContent;
    }

    public String getNewContent() {
        return newContent;
    }

    public int getAddedLines() {
        return addedLines;
    }

    public int getRemovedLines() {
        return removedLines;
    }

    public int getModifiedLines() {
        return modifiedLines;
    }
}
