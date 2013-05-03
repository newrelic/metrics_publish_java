package com.newrelic.platform.metrics.publish.configuration;

public class ConfigurationException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -4160697605517629312L;

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }

    
}
