package org.teamtators.common.config;

/**
 * Exception for configuration-related runtime issues
 */
public class ConfigException extends RuntimeException {
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigException(String message) {
        super(message);
    }
}
