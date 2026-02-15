package com.utagent.exception;

/**
 * Exception for test generation errors.
 */
public class GenerationException extends UTAgentException {

    public GenerationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public GenerationException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public GenerationException(String message, Throwable cause) {
        super(ErrorCode.GENERATION_ERROR, message, cause);
    }

    public GenerationException(String message) {
        super(ErrorCode.GENERATION_ERROR, message);
    }

    public static GenerationException generationFailed(String reason) {
        return new GenerationException(
            ErrorCode.GENERATION_ERROR,
            "Test generation failed: " + reason
        );
    }

    public static GenerationException llmError(String provider, Throwable cause) {
        return new GenerationException(
            ErrorCode.GENERATION_LLM_ERROR,
            "LLM provider error (" + provider + "): " + cause.getMessage(),
            cause
        );
    }

    public static GenerationException timeout(long timeoutMs) {
        return new GenerationException(
            ErrorCode.GENERATION_TIMEOUT,
            "Generation timeout after " + timeoutMs + "ms"
        );
    }
}
