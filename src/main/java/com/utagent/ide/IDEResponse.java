package com.utagent.ide;

public class IDEResponse {
    private final boolean success;
    private final String generatedCode;
    private final double coverageEstimate;
    private final String errorMessage;
    private final String testClassName;

    public IDEResponse(boolean success, String generatedCode, double coverageEstimate, 
                       String errorMessage, String testClassName) {
        this.success = success;
        this.generatedCode = generatedCode;
        this.coverageEstimate = coverageEstimate;
        this.errorMessage = errorMessage;
        this.testClassName = testClassName;
    }

    public static IDEResponse success(String code, double coverage, String testClassName) {
        return new IDEResponse(true, code, coverage, null, testClassName);
    }

    public static IDEResponse failure(String errorMessage) {
        return new IDEResponse(false, null, 0.0, errorMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public double getCoverageEstimate() {
        return coverageEstimate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getTestClassName() {
        return testClassName;
    }
}
