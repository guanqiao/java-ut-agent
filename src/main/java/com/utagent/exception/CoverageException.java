package com.utagent.exception;

import java.io.File;

/**
 * Exception for coverage analysis errors.
 */
public class CoverageException extends UTAgentException {

    public CoverageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public CoverageException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static CoverageException analysisFailed(Throwable cause) {
        return new CoverageException(
            ErrorCode.COVERAGE_ANALYSIS_ERROR,
            "Coverage analysis failed: " + cause.getMessage(),
            cause
        );
    }

    public static CoverageException reportNotFound(File reportFile) {
        return new CoverageException(
            ErrorCode.COVERAGE_REPORT_NOT_FOUND,
            "Coverage report not found: " + reportFile.getAbsolutePath()
        );
    }

    public static CoverageException executionFailed(int exitCode) {
        return new CoverageException(
            ErrorCode.COVERAGE_EXEC_FAILED,
            "Test execution failed with exit code: " + exitCode
        );
    }
}
