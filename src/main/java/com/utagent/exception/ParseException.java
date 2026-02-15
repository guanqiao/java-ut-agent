package com.utagent.exception;

import java.io.File;

/**
 * Exception for source code parsing errors.
 */
public class ParseException extends UTAgentException {

    public ParseException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ParseException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static ParseException fileNotFound(File file) {
        return new ParseException(
            ErrorCode.PARSE_FILE_NOT_FOUND,
            "Source file not found: " + file.getAbsolutePath()
        );
    }

    public static ParseException parseError(File file, Throwable cause) {
        return new ParseException(
            ErrorCode.PARSE_ERROR,
            "Failed to parse source file: " + file.getAbsolutePath(),
            cause
        );
    }

    public static ParseException unsupportedSyntax(String feature) {
        return new ParseException(
            ErrorCode.PARSE_UNSUPPORTED_SYNTAX,
            "Unsupported syntax: " + feature
        );
    }
}
