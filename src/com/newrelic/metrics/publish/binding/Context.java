package com.newrelic.metrics.publish.binding;

import java.io.File;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.newrelic.metrics.publish.configuration.SDKConfiguration;

/**
 * Provisional API which is subject to change.
 * The context for a {@link Request} that manages {@link AgentData} and {@link ComponentData}.
 */
public class Context {

    private static final String SERVICE_URI = "https://platform-api.newrelic.com/platform/v1/metrics";
    private static final String LOG_CONFIG_FILE = "logging.properties";
    private static final String LOGGER_NAME = "com.newrelic.metrics.publish";
    
    private static final String POST = "POST";
    private static final String X_LICENSE_KEY = "X-License-Key";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String ACCEPT = "Accept";
    private static final String AGENT = "agent";
    private static final String COMPONENTS = "components";

    private static final long AGGREGATION_LIMIT = TimeUnit.MINUTES.toMillis(20);
    private static final int CONNECTION_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(20);
    
    private static Logger LOGGER;
    
    public String licenseKey;
    public AgentData agentData;

    private String serviceURI = SERVICE_URI;
    private boolean sslHostVerification = true;
    private LinkedList<ComponentData> components;

    private Request lastRequest;
    private Date aggregationStartedAt;

    /**
     * Get a {@link java.util.logging.Logger} object for logging that is registered with the name 'com.newrelic.metrics.publish'.
     * <p> Developers should be aware that the provided logging framework may change in the future as the SDK changes.
     * <p> The logger first looks for a 'logging.properties' file for configuration.
     * If the configuration file cannot be found, the logger will be initialized with default {@link java.util.logging.Logger} properties.
     * The default behavior will use a {@link java.util.logging.ConsoleHandler} (System.err) and log at the INFO level.
     * The 'com.newrelic.metrics.publish' Logger is set to log level ALL so that it can be overridden by the
     * {@link java.util.logging.ConsoleHandler} and {@link java.util.logging.FileHandler} log levels which are specified in the
     * 'logging.properties' file.
     * @return Logger
     */
    public static Logger getLogger() {
        if (LOGGER == null) {
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
     * Log a variable length array of objects at a provided {@link java.util.logging.Level}. 
     * This will only concatenate the log messages if the log level is currently loggable.
     * <p> Ex. {@code Context.log(Level.FINE, "Name: ", name, ", Value: ", value);}
     * <p> Developers should be aware that the provided logging framework may change in the future as the SDK changes.
     * <p> See {@link #getLogger()} and {@link Logger#isLoggable(Level)} for additional information.
     * @param level
     * @param messages
     */
    public static void log(Level level, Object... messages) {
        if (getLogger().isLoggable(level)) {
            StringBuilder builder = new StringBuilder();
            for (Object message : messages) {
                builder.append(message);
            }
            getLogger().log(level, builder.toString());
        }
    }

    /**
     * Initializes the logger by looking for a 'logging.properties' file.
     * <p> See {@link #getLogger()} for additional information.
     */
    private static void initLogger() {
        InputStream inputStream = null;
        try {
        	String path = SDKConfiguration.getConfigDirectory() + File.separatorChar + LOG_CONFIG_FILE;
            inputStream = new FileInputStream(path);
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (SecurityException e) {
            System.err.println("WARNING: Logging is not currently configured. Please add a " + SDKConfiguration.getConfigDirectory() + "/logging.properties file to enable additional logging.");
        } catch (IOException e) {
            System.err.println("WARNING: Logging is not currently configured. Please add a " + SDKConfiguration.getConfigDirectory() + "/logging.properties file to enable additional logging.");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    System.err.println("WARNING: An error has occurred initializing logging. Please add a " + SDKConfiguration.getConfigDirectory() + "/logging.properties file to enable additional logging.");
                    System.err.println(e);
                }
            }
        }
        Logger logger = Logger.getLogger(LOGGER_NAME);
        // setting handler's formatter to a custom formatter
        for(Handler handler : logger.getHandlers()) {
            handler.setFormatter(new LogFormatter());
        }
        // setting the logger's level to the highest handler level so that it
        // can be overridden by the ConsoleHandler and FileHandler log levels.
        Level level = getInitialLogLevel(logger.getHandlers());
        logger.setLevel(level);
        setLogger(logger);
    }
    
    /* package */ static Level getInitialLogLevel(Handler[] handlers) {
        Level level = Level.INFO;
        for(Handler handler : handlers) {
            handler.setFormatter(new LogFormatter());
            if (handler.getLevel().intValue() < level.intValue()) {
                level = handler.getLevel();
            }
        }
        return level;
    }

    /**
     * Constructs a {@code Context}
     */
    public Context() {
        super();
        agentData = new AgentData();
        components = new LinkedList<ComponentData>();
        lastRequest = new Request(this);
        aggregationStartedAt = new Date();
    }

    /**
     * Create a {@link Request}.
     * If the last {@code Request} was not sent successfully, the last {@code Request} will be reused.
     * This guarantees that previously reported metrics will be aggregated with new metrics, and
     * no metric data will be lost if a request was not sent successfully.
     * @return request
     */
    public Request createRequest() {
        if (isPastAggregationLimit()) {
            lastRequest = new Request(this);
            for (ComponentData component : components) {
                component.setLastSuccessfulReportedAt(null);
            }
        }
        else if (isLastRequestDelivered()) {
            lastRequest = new Request(this);
        }
        return lastRequest;
    }

    private boolean isLastRequestDelivered() {
        return lastRequest.isDelivered();
    }

    /* package */ boolean isPastAggregationLimit() {
        if (aggregationStartedAt != null) {
            long aggregationDuration = new Date().getTime() - aggregationStartedAt.getTime();
            return aggregationDuration > AGGREGATION_LIMIT;
        }
        return false;
    }

    /* package */ void setAggregationStartedAt(Date aggregationStartedAt) {
        this.aggregationStartedAt = aggregationStartedAt;
    }

    /* package */ Date getAggregationStartedAt() {
        return aggregationStartedAt;
    }

    /**
     * Create a {@link ComponentData} that reported metrics will belong to.
     * @return ComponentData
     */
    public ComponentData createComponent() {
        ComponentData componentData = new ComponentData();
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
        log(Level.FINE, "Metric service url: ", serviceUrl);

        HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection();
        connection.setRequestMethod(POST);
        connection.addRequestProperty(X_LICENSE_KEY, licenseKey);
        connection.addRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
        connection.addRequestProperty(ACCEPT, APPLICATION_JSON);
        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(CONNECTION_TIMEOUT);

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
        output.put(AGENT, agentData.serialize());

        LinkedList<HashMap<String, Object>> componentsOutput = new LinkedList<HashMap<String, Object>>();
        output.put(COMPONENTS, componentsOutput);

        for (ComponentData component : components) {
            HashMap<String, Object> map = component.serialize(request);

            if(!map.isEmpty()) {
                componentsOutput.add(map);
            }
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