package com.newrelic.metrics.publish.configuration;

import com.newrelic.metrics.publish.util.Logger;

/**
 * An object representation of the data read from the {@code newrelic.properties} configuration file.
 * <p>
 * This class is now deprecated and will be removed in a future release. See {@link Config}.
 * @see Config
 */
@Deprecated
public class SDKConfiguration {

    private static final Logger logger = Logger.getLogger(SDKConfiguration.class);
    
    private static final String DEFAULT_LICENSE_KEY = "YOUR_LICENSE_KEY_HERE";

    private String licenseKey;
    private String serviceURI;
    private boolean sslHostVerification = true;

    /**
     * Constructs a {@code SDKConfiguration}
     * @throws ConfigurationException if there is an error reading the {@code newrelic.json} file
     */
    public SDKConfiguration() throws ConfigurationException {

        licenseKey = Config.getValue("license_key");
        if (null == licenseKey) {
            throw new ConfigurationException("license_key is undefined");
        }

        if (licenseKey.equals(DEFAULT_LICENSE_KEY)) {
            throw new ConfigurationException("You forgot to update the New Relic license_key from '" + DEFAULT_LICENSE_KEY + "'. " +
            		"See https://docs.newrelic.com/docs/subscriptions/license-key for more information.");
        }

        // endpoint is optional and used for debugging
        if (Config.getValue("endpoint") != null) {
            serviceURI = Config.getValue("endpoint");
        }

        if (Config.getValue("ssl_host_verification") != null) {
            sslHostVerification = (Boolean) Config.getValue("ssl_host_verification");
            logger.debug("Using SSL host verification: ", sslHostVerification);
        }
        
        initProxySettings();
    }

    /**
     * Get the license key
     * @return String
     */
    public String getLicenseKey() {
        return licenseKey;
    }

    /**
     * Set the license key
     * @param key
     */
    public void setLicenseKey(String key) {
        licenseKey = key;
    }

    /**
     * Get the poll interval
     * @return int the poll interval
     */
    public int getPollInterval() {
        return 60;
    }

    /**
     * For debug purposes only, not for general usage by clients of the SDK
     */
    public String internalGetServiceURI() {
        return serviceURI;
    }

    /**
     * Returns if ssl host verification is enabled.
     * Adding {@code sslHostVerification} to {@code newrelic.properties}. It is {@code true} by default.
     * @return boolean
     */
    public boolean isSSLHostVerificationEnabled() {
        return sslHostVerification;
    }

    /**
     * Now deprecated. Will be removed in a future release. See {@link Config#getConfigDirectory()}
     */
    @Deprecated
    public static String getConfigDirectory() {
        return Config.getConfigDirectory();
    }
    
    private void initProxySettings() throws ConfigurationException {
        String protocol = getServiceURIProtocol();
        
        if (Config.getValue("proxy_host") != null) {
            System.setProperty(protocol + ".proxyHost", Config.<String>getValue("proxy_host"));
            logger.info("Using proxy host: ", Config.<String>getValue("proxy_host"));
        }
        
        if (Config.getValue("proxy_port") != null) {
            if ( !(Config.getValue("proxy_port") instanceof String) ) {
                throw new ConfigurationException("'proxy_port' must be a String");
            } else {
                System.setProperty(protocol + ".proxyPort", Config.<String>getValue("proxy_port"));
                logger.info("Using proxy port: ", Config.<String>getValue("proxy_port"));
            }
        }
        
        if (Config.getValue("proxy_username") != null) {
            System.setProperty(protocol + ".proxyUser", Config.<String>getValue("proxy_username"));
            logger.info("Using proxy username: ", Config.<String>getValue("proxy_username"));
        }
        
        if (Config.getValue("proxy_password") != null) {
            System.setProperty(protocol + ".proxyPassword", Config.<String>getValue("proxy_password"));
            logger.info("Using proxy password: [REDACTED]");
        }
    }
    
    private String getServiceURIProtocol() {
        String protocol = "https";
        if (serviceURI != null) {
            protocol = serviceURI.startsWith("https") ? "https" : "http";
        }
        return protocol;
    }
}
