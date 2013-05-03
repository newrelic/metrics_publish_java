package com.newrelic.metrics.publish.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.newrelic.metrics.publish.binding.Context;



/**
 * An object representation of the data read from the newrelic.properties configuration file.
 */
public class SDKConfiguration {

	private static final String DEFAULT_LICENSE_KEY = "YOUR_LICENSE_KEY_HERE";
    private String licenseKey;
    private String serviceURI;
	private final String propertyFileName = "newrelic.properties";
	private final String configPath = "config";
	
    public SDKConfiguration() throws IOException, ConfigurationException {        
        File file = getConfigurationFile();
        
        Context.getLogger().info("Using configuration file " + file.getAbsolutePath());
        
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
        	Context.getLogger().info("Metric service URI: " + serviceURI);
        }
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String key) {
        licenseKey = key;
    }

    public int getPollInterval() {
        return 60;
    }
    
	/*
	 * For debug purposes only, not for general usage by clients of the SDK
	 */
	public String internalGetServiceURI() {
		return serviceURI;
	}
	
    /* 
	 * For debug purposes only, not for general usage by clients of the SDK
     */
    public void internalProcessDebugArgs(String[] args) {
		//first is the license key
    	if(args.length > 0) {
    		setLicenseKey(args[0]);
    	}
		//second is the collector
    	if(args.length == 2) {
    		serviceURI = args[1];
    	}    		
    }
    
    private File getConfigurationFile() throws ConfigurationException {
//		TODO system property for config path
//      String path = System.getProperty("com.newrelic.platform.config");        
    	String path = null;
        if (path == null) {
            path = configPath + File.separatorChar + propertyFileName;
            File file = new File(path);
            if (!file.exists()) {
            	throw logAndThrow("Cannot find config file " + path);
            }
            return file;
        } else {
            return new File(path);    
        }
	}
    
    private ConfigurationException logAndThrow(String message) {
    	Context.getLogger().severe(message);
        return new ConfigurationException(message);
    }
}
