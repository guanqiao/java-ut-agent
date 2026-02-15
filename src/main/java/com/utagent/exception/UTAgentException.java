package com.utagent.exception;

/**
 * Base exception for all Java UT Agent errors.
 */
public class UTAgentException extends RuntimeException {

    private final ErrorCode errorCode;

    public UTAgentException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public UTAgentException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Error codes for different types of failures.
     */
    public enum ErrorCode {
        // Configuration errors (1xx)
        CONFIG_NOT_FOUND(101, "Configuration file not found"),
        CONFIG_PARSE_ERROR(102, "Failed to parse configuration"),
        CONFIG_INVALID(103, "Invalid configuration"),
        CONFIG_ERROR(104, "Configuration error"),

        // Parsing errors (2xx)
        PARSE_ERROR(201, "Failed to parse source code"),
        PARSE_FILE_NOT_FOUND(202, "Source file not found"),
        PARSE_UNSUPPORTED_SYNTAX(203, "Unsupported syntax"),

        // Generation errors (3xx)
        GENERATION_ERROR(301, "Test generation failed"),
        GENERATION_LLM_ERROR(302, "LLM provider error"),
        GENERATION_TIMEOUT(303, "Generation timeout"),

        // Coverage errors (4xx)
        COVERAGE_ANALYSIS_ERROR(401, "Coverage analysis failed"),
        COVERAGE_REPORT_NOT_FOUND(402, "Coverage report not found"),
        COVERAGE_EXEC_FAILED(403, "Failed to execute tests for coverage"),
        COVERAGE_ERROR(404, "Coverage error"),

        // Build tool errors (5xx)
        BUILD_TOOL_NOT_DETECTED(501, "Build tool not detected"),
        BUILD_COMMAND_FAILED(502, "Build command failed"),
        BUILD_ERROR(503, "Build error"),

        // Optimization errors (6xx)
        OPTIMIZATION_ERROR(601, "Optimization failed"),
        OPTIMIZATION_MAX_ITERATIONS(602, "Max iterations reached without meeting target"),

        // IO errors (7xx)
        IO_ERROR(701, "IO operation failed"),
        FILE_NOT_FOUND(702, "File not found"),
        FILE_WRITE_ERROR(703, "Failed to write file"),
        FILE_READ_ERROR(704, "Failed to read file"),

        // LLM errors (8xx)
        LLM_ERROR(801, "LLM service error"),

        // Validation errors (9xx)
        VALIDATION_ERROR(901, "Validation failed"),
        TIMEOUT_ERROR(902, "Operation timed out"),
        RESOURCE_ERROR(903, "Resource error"),

        // General errors (999)
        UNKNOWN_ERROR(999, "Unknown error");

        private final int code;
        private final String description;

        ErrorCode(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}
