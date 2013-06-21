package com.newrelic.metrics.publish.binding;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * The context for a {@link Request} that manages {@link AgentData} and {@link ComponentData}.
 */
public class Context {

	private static final String SERVICE_URI = "https://platform-api.newrelic.com/platform/v1/metrics";
	private static final String LOG_CONFIG_FILE = "config/logging.properties";
	private static final String LOGGER_NAME = "com.newrelic.metrics.publish";
	
	public String licenseKey;
	public AgentData agentData;
	
	private String serviceURI = SERVICE_URI;
	private boolean sslHostVerification = true;
    private static Logger LOGGER;
	private LinkedList<ComponentData> components;
	
	/**
	 * Get a {@link java.util.logging.Logger} object for logging that is registered with the name 'com.newrelic.metrics.publish'.
	 * <p> Developers should be aware that the provided logging framework may change in the future as the SDK changes. 
	 * <p> The logger first looks for a 'config/logging.properties' file for configuration.
	 * If the configuration file cannot be found, the logger will be initialized with default {@link java.util.logging.Logger} properties.
	 * The default behavior will use a {@link java.util.logging.ConsoleHandler} (System.err) and log at the INFO level.
	 * The 'com.newrelic.metrics.publish' Logger is set to log level ALL so that it can be overridden by the 
	 * {@link java.util.logging.ConsoleHandler} and {@link java.util.logging.FileHandler} log levels which are specified in the
	 * 'config/logging.properties' file.
	 * @return Logger
	 */
	public static Logger getLogger() {
		if(LOGGER == null) {
			initLogger();
		}
		return LOGGER;
	}
	
	/**
     * Set a {@link java.util.logging.Logger} object. 
     * This method should only be called to override the default logger settings.
     * @param logger the {@link java.util.logging.Logger} to set
     */
	public static void setLogger(Logger logger) {
		LOGGER = logger;
	}
	
	/**
	 * Initializes the logger by looking for a 'config/logging.properties' file.
	 * <p> See {@link #getLogger()} for additional information.
	 */
	private static void initLogger() {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(LOG_CONFIG_FILE);
			LogManager.getLogManager().readConfiguration(inputStream);
		} catch (SecurityException e) {
			System.err.println("WARNING: Logging is not currently configured. Please add a config/logging.properties file to enable additional logging.");
		} catch (IOException e) {
			System.err.println("WARNING: Logging is not currently configured. Please add a config/logging.properties file to enable additional logging.");
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					System.err.println("WARNING: An error has occurred initializing logging. Please add a config/logging.properties file to enable additional logging.");
					System.err.println(e);
				}
			}
		}
		Logger logger = Logger.getLogger(LOGGER_NAME);
		// setting handler's formatter to a custom formatter
		for(Handler handler : logger.getHandlers()) {
		    handler.setFormatter(new LogFormatter());
		}
		// setting the logger's level to ALL so that it can be overridden by the ConsoleHandler and FileHandler log levels.
		logger.setLevel(Level.ALL);	
		setLogger(logger);
	}
	
	/**
     * Constructs a {@code Context}
     */
	public Context() {
		super();
		agentData = new AgentData();
		components = new LinkedList<ComponentData>();
	}
	
	/**
     * Create a {@link ComponentData} that reported metrics will belong to.
     * @return ComponentData
     */
	public ComponentData createComponent() {
		ComponentData componentData = new ComponentData(this);
		add(componentData);
		return componentData;
	}

	/**
     * Get an {@link Iterator} for the list of {@link ComponentData}
     * @return Iterator
     */
	public Iterator<ComponentData> getComponents() {
		return components.iterator();
	}
	
	/**
     * Return the URI of the metric data service that metric data gets posted to.
     */
	public String getServiceURI() {
		return serviceURI;
	}
	
	/**
     * An internal method for debug purposes only, not for general usage by clients of the SDK
     */
	public void internalSetServiceURI(String URI) {
		serviceURI = URI;
	}

	/**
	 * Internal method for setting ssl host verification
	 * @param sslHostVerification
	 */
    public void internalSetSSLHostVerification(boolean sslHostVerification) {
        this.sslHostVerification = sslHostVerification;
    }

    /* package */ void add(ComponentData componentData) {
		components.add(componentData);
	}
	
    /**
     * Create an http url connection to post data to the New Relic service.
     *  
     * @return HttpURLConnection
     * @throws IOException
     */
    /* package */ HttpURLConnection createUrlConnectionForOutput() throws IOException {
        URL serviceUrl = new URL(serviceURI);
        LOGGER.fine("Metric service url: " + serviceUrl);
        
        HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.addRequestProperty("X-License-Key", licenseKey);
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("Accept", "application/json");
        
        // if not verifying ssl host and using https, add custom hostname verifier
        // else use default hostname verifier
        if (connection instanceof HttpsURLConnection && !sslHostVerification) {
            // ssl hostname verifier verifies any host
            ((HttpsURLConnection) connection).setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }
        
        connection.setDoOutput(true);
        return connection;
    }
	
	/* package */ Map<String, Object> serialize(Request request) {		
        Map<String, Object> output = new HashMap<String, Object>();
        output.put("agent", agentData.serialize());
        		
		LinkedList<HashMap<String, Object>> componentsOutput = new LinkedList<HashMap<String, Object>>();
		output.put("components", componentsOutput);
		
		for (ComponentData component : components) {
			componentsOutput.add(component.serialize(request));
		}
		
		return output;
	}	
	
	/**
	 * Using the source of {@link java.util.logging.SimpleFormatter} as a starting point.
	 * LogFormatter adjusts the line formatting for each log message. 
	 * Better timestamp formatting and shrinking the log statement to one line.
	 */
	static private class LogFormatter extends Formatter {
	    private static final String LEFT_BRACKET = "[";
	    private static final String RIGHT_BRACKET = "]";
	    private static final String SPACE = " ";
	    private static final String PIPE = "|";
	    
	    Date date = new Date();
	    private final static String format = "{0,date,yyyy-MM-dd} {0,time,HH:mm:ss Z}";
	    private MessageFormat formatter;
	    
	    private Object args[] = new Object[1];
	    
	    private String lineSeparator = System.getProperty("line.separator");
	    
	    /**
	     * Format the given LogRecord.
	     * @param record the log record to be formatted.
	     * @return a formatted log record
	     */
        @Override
        public synchronized String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            builder.append(LEFT_BRACKET);
            // Minimize memory allocations here.
            date.setTime(record.getMillis());
            args[0] = date;
            StringBuffer text = new StringBuffer();
            if (formatter == null) {
                formatter = new MessageFormat(format);
            }
            formatter.format(args, text, null);
            builder.append(text);
            builder.append(RIGHT_BRACKET).append(SPACE);
            if (record.getSourceClassName() != null) {  
                builder.append(record.getSourceClassName());
            } else {
                builder.append(record.getLoggerName());
            }
            builder.append(SPACE).append(PIPE).append(SPACE);
            builder.append(record.getLevel().getLocalizedName());
            builder.append(SPACE).append(PIPE).append(SPACE);
            String message = formatMessage(record);
            builder.append(message);
            builder.append(lineSeparator);
            if (record.getThrown() != null) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    builder.append(sw.toString());
                } catch (Exception ex) {}
            }
            return builder.toString();
        }
	}
}