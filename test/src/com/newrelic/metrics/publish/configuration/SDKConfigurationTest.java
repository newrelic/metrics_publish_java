package com.newrelic.metrics.publish.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class SDKConfigurationTest {

    @Before
    public void setup() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");
        
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("https.proxyUser");
        System.clearProperty("https.proxyPassword");
    }
    
    @Test
    public void testDefaultProxySettings() throws ConfigurationException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("license_key", "required_license_key");
        config.put("endpoint", "http://test_url");
        
        Config.init(config);
        
        SDKConfiguration sdkConfig = new SDKConfiguration();
        
        assertNull(System.getProperty("http.proxyHost"));
        assertNull(System.getProperty("http.proxyPort"));
        assertNull(System.getProperty("http.proxyUser"));
        assertNull(System.getProperty("http.proxyPassword"));
        
        assertNull(System.getProperty("https.proxyHost"));
        assertNull(System.getProperty("https.proxyPort"));
        assertNull(System.getProperty("https.proxyUser"));
        assertNull(System.getProperty("https.proxyPassword"));
    }
    
    @Test
    public void testHTTPProxySettings() throws ConfigurationException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("license_key", "required_license_key");
        config.put("endpoint", "http://test_url");
        config.put("proxy_host", "test_proxy_host");
        config.put("proxy_port", "80");
        config.put("proxy_username", "test_proxy_user_bob");
        config.put("proxy_password", "test_proxy_pass_5");
        
        Config.init(config);
        
        SDKConfiguration sdkConfig = new SDKConfiguration();
        
        assertEquals("test_proxy_host", System.getProperty("http.proxyHost"));
        assertEquals("80", System.getProperty("http.proxyPort"));
        assertEquals("test_proxy_user_bob", System.getProperty("http.proxyUser"));
        assertEquals("test_proxy_pass_5", System.getProperty("http.proxyPassword"));
    }
    
    @Test
    public void testHTTPSProxySettings() throws ConfigurationException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("license_key", "required_license_key");
        config.put("endpoint", "https://test_url");
        config.put("proxy_host", "test_proxy_host");
        config.put("proxy_port", "431");
        config.put("proxy_username", "test_proxy_user_bob");
        config.put("proxy_password", "test_proxy_pass_5");
        
        Config.init(config);
        
        SDKConfiguration sdkConfig = new SDKConfiguration();
        
        assertEquals("test_proxy_host", System.getProperty("https.proxyHost"));
        assertEquals("431", System.getProperty("https.proxyPort"));
        assertEquals("test_proxy_user_bob", System.getProperty("https.proxyUser"));
        assertEquals("test_proxy_pass_5", System.getProperty("https.proxyPassword"));
    }
    
    @Test
    public void testNullProxySettings() throws ConfigurationException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("license_key", "required_license_key");
        config.put("proxy_host", null);
        
        Config.init(config);
        
        SDKConfiguration sdkConfig = new SDKConfiguration();
        
        assertNull(System.getProperty("https.proxyHost"));
    }
    
    @Test(expected = ConfigurationException.class)
    public void testNumericProxyPortThrowsAnException() throws ConfigurationException {
        Map<String, Object> config = new HashMap<String, Object>();
        config.put("license_key", "required_license_key");
        config.put("proxy_port", 3128);
        
        Config.init(config);
        
        SDKConfiguration sdkConfig = new SDKConfiguration();
    }
}
