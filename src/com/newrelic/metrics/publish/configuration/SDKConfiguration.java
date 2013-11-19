package com.newrelic.metrics.publish.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import com.newrelic.metrics.publish.binding.Context;



/**
 * An object representation of the data read from the {@code newrelic.properties} configuration file.
 */
public class SDKConfiguration {

    private static final String CONFIG_PROPERTY = "newrelic.platform.config.dir";
	private static final String DEFAULT_LICENSE_KEY = "YOUR_LICENSE_KEY_HERE";
	private static final String propertyFileName = "newrelic.properties";
    private static final String configPath = "config";
	
    private String licenseKey;
    private String serviceURI;
    private boolean sslHostVerification = true;
	
	/**
     * Constructs a {@code SDKConfiguration}
     * @throws IOException if the {@code newrelic.properties} file does not exist
     * @throws ConfigurationException if there is an error reading the {@code newrelic.properties} file
     */
    public SDKConfiguration() throws IOException, ConfigurationException {        
        File file = getConfigurationFile();
        
        System.out.println("INFO: Using configuration file " + file.getAbsolutePath());
        
        if (!file.exists()) {
            throw logAndThrow(file.getAbsolutePath() + " does not exist");
        }
        
        Properties props = new Properties();
        InputStream inStream = new FileInputStream(file);
        try {
        	props.load(inStream);
        } finally {
        	inStream.close();
        }
        
        licenseKey = props.getProperty("licenseKey");
        if (null == licenseKey) {
            throw logAndThrow("licenseKey is undefined");
        }
        
        if(licenseKey.equals(DEFAULT_LICENSE_KEY)) {
            throw logAndThrow("You forgot to update the licenseKey from '" + DEFAULT_LICENSE_KEY + "'");        	
        }
        
        //host is optional and for debugging
        if(props.containsKey("host")) {
        	serviceURI = props.getProperty("host");
        	Context.log(Level.INFO, "Metric service URI: ", serviceURI);
        }
        
        if (props.containsKey("sslHostVerification")) {
            sslHostVerification = Boolean.parseBoolean(props.getProperty("sslHostVerification"));
            Context.log(Level.FINE, "Using SSL host verification: ", sslHostVerification);
        }
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
	 * Use the 'newrelic.platform.config.dir' jvm option to set the configuration directory
	 */
	public static String getConfigDirectory() {
		String path = System.getProperty(CONFIG_PROPERTY);
		if (path == null) {
			path = configPath;
		}
		return path;
	}
    
    private File getConfigurationFile() throws ConfigurationException {
    	String path = getConfigDirectory() + File.separatorChar + propertyFileName;
    	File file = new File(path);
    	if (!file.exists()) {
    		throw logAndThrow("Cannot find config file " + path);
    	}
    	return file;
	}
    
    private ConfigurationException logAndThrow(String message) {
        Context.log(Level.SEVERE, message);
        return new ConfigurationException(message);
    }
}
