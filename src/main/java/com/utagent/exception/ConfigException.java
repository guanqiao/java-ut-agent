package com.utagent.exception;

/**
 * Exception for configuration-related errors.
 */
public class ConfigException extends UTAgentException {

    public ConfigException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ConfigException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static ConfigException notFound(String configPath) {
        return new ConfigException(
            ErrorCode.CONFIG_NOT_FOUND,
            "Configuration file not found: " + configPath
        );
    }

    public static ConfigException parseError(String configPath, Throwable cause) {
        return new ConfigException(
            ErrorCode.CONFIG_PARSE_ERROR,
            "Failed to parse configuration file: " + configPath,
            cause
        );
    }

    public static ConfigException invalid(String message) {
        return new ConfigException(
            ErrorCode.CONFIG_INVALID,
            "Invalid configuration: " + message
        );
    }
}
