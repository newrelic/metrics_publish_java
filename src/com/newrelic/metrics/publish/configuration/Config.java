package com.newrelic.metrics.publish.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.newrelic.metrics.publish.Runner;

/**
 * Class for accessing config values defined in the {@code newrelic.json} and {@code plugin.json}
 * configuration files.
 * It is initialized when a {@link Runner} is a created.
 * @see {@link #getValue(String)} and {@link #getValue(String, Object)}
 */
public final class Config {
    
    private static final String CONFIG_PROPERTY = "newrelic.platform.config.dir";
    private static final String CONFIG_PATH = "config";
    
    private static final String NEW_RELIC_CONFIG_FILE = getConfigDirectory() + File.separator + "newrelic.json";
    private static final String PLUGIN_CONFIG_FILE = getConfigDirectory() + File.separator + "plugin.json";
    private static final String SDK_VERSION = "2.0.0";
    
    private static Map<String, Object> config = new HashMap<String, Object>();
    
    private Config() {}
    
    /**
     * This class must be initialized before accessing config values.
     * @throws ConfigurationException
     */
    public static void init() throws ConfigurationException {
        try {
            load(NEW_RELIC_CONFIG_FILE);
            load(PLUGIN_CONFIG_FILE, false);
        } catch(IOException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }
    
    /**
     * Get config value.
     * Config values are loaded from the {@code newrelic.json} and {@code plugin.json} configuration files.
     * <p>
     * Supported values are {@link String Strings}, {@link Boolean Booleans}, {@link Long Longs}, {@link Double Doubles}, 
     * {@link Map Maps}, and {@link List Lists}.
     * <p>
     * Examples:
     * <p>
     * <pre>
     * {@code
     * String username = Config.getValue("username");
     * Long maxValue = Config.getValue("max_value");
     * Map<String, Object> categories = Config.getValue("categories");
     * }
     * 
     * @param property
     * @return config value of type T, null if not found
     * @see #getValue(String, Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValue(String property) {
        return (T) config.get(property);
    }
    
    /**
     * Get config value with optional default value.
     * Config values are loaded from the {@code newrelic.json} and {@code plugin.json} configuration files.
     * <p>
     * Supported values are {@link String Strings}, {@link Boolean Booleans}, {@link Long Longs}, {@link Double Doubles}, 
     * {@link Map Maps}, and {@link List Lists}.
     * <p>
     * Examples:
     * <p>
     * <pre>
     * {@code
     * String username = Config.getValue("username", "user1");
     * Long maxValue = Config.getValue("max_value", 25);
     * List<Integer> values = Config.getValue("values", Arrays.asList(1, 2, 3));
     * }
     * 
     * @param property
     * @param defaultValue
     * @return config value of type T if defined, defaultValue otherwise
     * @see #getValue(String)
     */
    public static <T> T getValue(String property, T defaultValue) {
        T value = Config.<T>getValue(property);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Use the {@code newrelic.platform.config.dir} JVM option to set the configuration directory
     */
    public static String getConfigDirectory() {
        String path = System.getProperty(CONFIG_PROPERTY);
        return (path != null) ? path : CONFIG_PATH;
    }
    
    public static String getSdkVersion() {
        return SDK_VERSION;
    }
    
    static void load(String filePath) throws IOException {
        load(filePath, true);
    }
    
    static void load(String filePath, boolean required) throws IOException {
        File configFile = new File(filePath);
        if (configFile.exists()) {
            load(configFile);
        }
        else if (required) {
            throw new FileNotFoundException("Configuration file is missing: " + filePath);
        }
        else {
            System.out.println("WARNING: Optional configuration file is missing: " + filePath);
        }
    }
    
    /*
     * Suppressing warnings due to JSONObject implementing map without generic types
     */
    @SuppressWarnings("unchecked")
    static void load(File file) throws IOException {
        System.out.println("INFO: Using configuration file " + file.getAbsolutePath());
        Reader reader = new FileReader(file);
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
            
            config.putAll(jsonObject);
            
        } catch (ParseException e) {
            throw new IOException(e);
        } finally {
            if (reader != null) reader.close();
        }
    }
    
    /*
     * For testing
     */
    static void init(Map<String, Object> initialConfig) {
        config = initialConfig;
    }

}
