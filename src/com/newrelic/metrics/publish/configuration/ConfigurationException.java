package com.newrelic.metrics.publish.configuration;

/**
 * A general exception for SDK configuration issues
 */
public class ConfigurationException extends Exception {

    private static final long serialVersionUID = -4160697605517629312L;

    /**
     * Constructs a {@code ConfigurationException} with a message and cause
     * @param message
     * @param cause
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code ConfigurationException} with a message
     * @param message
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code ConfigurationException} with a cause
     * @param cause
     */
    public ConfigurationException(Throwable cause) {
        super(cause);
    }

    
}
