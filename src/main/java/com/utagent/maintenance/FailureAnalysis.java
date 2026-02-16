package com.utagent.maintenance;

public class FailureAnalysis {

    private final FailureType failureType;
    private final String rootCause;
    private final String affectedCode;
    private final int lineNumber;

    public FailureAnalysis(FailureType failureType, String rootCause, String affectedCode, int lineNumber) {
        this.failureType = failureType;
        this.rootCause = rootCause;
        this.affectedCode = affectedCode;
        this.lineNumber = lineNumber;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public String getRootCause() {
        return rootCause;
    }

    public String getAffectedCode() {
        return affectedCode;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
