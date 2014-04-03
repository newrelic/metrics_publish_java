package com.newrelic.metrics.publish.util;

import java.io.File;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;

/**
 * Logger class which supports logging at debug, info, warn, error, and fatal levels. 
 * By default the log level is info. 
 * 
 * Logging is directed to the console and to a log file. The log file name and location is configurable.
 * 
 * <p>
 * For usage see, {@link #getLogger(Class)}
 * 
 * <p>
 * Note: {@link #init(String, String, String, Integer)} must be called before getting a logger via {@link #getLogger(Class)}.
 * Logger initialization is managed by the SDK.
 */
public final class Logger {

    /**
     * Supported log levels:
     * Debug, Info, Warn, Error, Fatal
     */
    public enum Level {
        Debug, Info, Warn, Error, Fatal;
        
        public static Level fromString(String value) {
            for (Level level : Level.values()) {
                if (level.toString().equalsIgnoreCase(value)) {
                    return level;
                }
            }
            return null;
        }
    }
    
    // default configuration
    private static Level level = Level.Info;
    private static String filePath = "logs";
    private static String fileName = "newrelic_plugin.log";
    private static Integer fileLimitInKilobytes = 25600; // 25 MB
    
    // logback configuration
    private static final String LogPattern  = "[%date] %-5level %logger - %msg%n";
    private static final ConsoleAppender<ILoggingEvent> ConsoleAppender = new ConsoleAppender<ILoggingEvent>();
    private static final RollingFileAppender<ILoggingEvent> FileAppender = new RollingFileAppender<ILoggingEvent>();
    
    private final ch.qos.logback.classic.Logger logger;
    
    private Logger(ch.qos.logback.classic.Logger logger, Level level) {
        this.logger = logger;
        this.logger.addAppender(ConsoleAppender);
        this.logger.addAppender(FileAppender);
        this.logger.setLevel(translateLevel(level));
    }
    
    /**
     * Get a logger for a specific class.
     * <p>
     * For better visibility into where log messages are reported from, 
     * it is recommended to create a static logger once per class.
     * 
     * <p>
     * Examples:
     * <p>
     * <pre>
     * {@code
     * private static final Logger logger = Logger.getLogger(ExampleAgent.class);
     * logger.debug("debug log message");
     * logger.error(new RuntimeError(), "error log message", "\tsecond message");
     * }
     * <p>
     * 
     * @param klass the class name will be used for the name of the logger
     * @return Logger
     */
    public static Logger getLogger(Class<?> klass) {
        return new Logger((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(klass), getLevel());
    }
    
    /**
     * The Logger class must be initialized before any loggers can be created.
     * 
     * <p>
     * If logFileLimitInKilobytes is 0, log file max size and file rolling will be disabled. 
     * The log file size would then be unlimited.
     * 
     * @param logLevel the string log level for all loggers
     * @param logFilePath the path to the log file for all loggers
     * @param logFileName the log file name for all loggers
     * @param logFileLimitInKilobytes the max size of the log file in kilobytes
     */
    public static void init(String logLevel, String logFilePath, String logFileName, Integer logFileLimitInKilobytes) {
        validateArgs(logLevel, logFilePath, logFileName, logFileLimitInKilobytes);
        
        Logger.level = Level.fromString(logLevel);
        Logger.filePath = logFilePath;
        Logger.fileName = logFileName;
        Logger.fileLimitInKilobytes = logFileLimitInKilobytes;
        
        initLogback();
    }
    
    private static void validateArgs(String logLevel, String logFilePath, String logFileName, Integer logFileLimitInKilobytes) {
        if (isNullOrEmptyString(logLevel)) {
            throw new IllegalArgumentException("'logLevel' must not be null or empty");
        }
        if (isNullOrEmptyString(logFilePath)) {
            throw new IllegalArgumentException("'logFilePath' must not be null or empty");
        }
        if (isNullOrEmptyString(logFileName)) {
            throw new IllegalArgumentException("'logFileName' must not be null or empty");
        }
        if (logFileLimitInKilobytes == null || logFileLimitInKilobytes < 0) {
            throw new IllegalArgumentException("'logFileLimitInKilobytes' must not be null or negative");
        }
    }
    
    private static boolean isNullOrEmptyString(String value) {
        return value == null || value.length() == 0;
    }
    
    /**
     * Initialize Logback
     */
    private static void initLogback() {
        // reset logger context
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        
        // shared console appender
        ConsoleAppender.setContext(context);
        ConsoleAppender.setTarget("System.out");
        
        PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
        consoleEncoder.setContext(context);
        consoleEncoder.setPattern(LogPattern);
        consoleEncoder.start();
        ConsoleAppender.setEncoder(consoleEncoder);
        ConsoleAppender.start();
        
        // rolling file
        String logFile = getFilePath() + File.separatorChar + getFileName();
        FileAppender.setContext(context);
        FileAppender.setFile(logFile);
        
        // log pattern
        PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
        fileEncoder.setContext(context);
        fileEncoder.setPattern(LogPattern);
        fileEncoder.start();
        FileAppender.setEncoder(fileEncoder);
        
        // rolling policy
        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(FileAppender);
        rollingPolicy.setFileNamePattern(logFile + "%i.zip");
        rollingPolicy.setMinIndex(1);
        rollingPolicy.setMaxIndex(1);
        rollingPolicy.start();
        
        // file max size - if fileLimit is 0, set max file size to maximum allowed
        long fileLimit = getFileLimitInKBytes() != 0 ? getFileLimitInKBytes() * 1024 : Long.MAX_VALUE;
        SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<ILoggingEvent>(String.valueOf(fileLimit));
        triggeringPolicy.start();
        
        FileAppender.setRollingPolicy(rollingPolicy);
        FileAppender.setTriggeringPolicy(triggeringPolicy);
        FileAppender.start();
    }
    
    /**
     * Log a message with a variable number of arguments at the debug level.
     * Only logs if the debug level is enabled.
     * 
     * @param messages
     * @throws IllegalArgumentException if messages is null
     */
    public void debug(Object... messages) {
        if (logger.isDebugEnabled()) {
            logger.debug(buildMessage(messages));
        }
    }
    
    /**
     * Log a throwable and a message with a variable number of arguments at the debug level.
     * Only logs if the debug level is enabled.
     * 
     * @param throwable
     * @param messages
     * @throws IllegalArgumentException if messages is null
     */
    public void debug(Throwable throwable, Object... messages) {
        if (logger.isDebugEnabled()) {
            logger.debug(buildMessage(messages), throwable);
        }
    }
    
    /**
     * Log a message with a variable number of arguments at the info level.
     * Only logs if the info level is enabled.
     * 
     * @param messages
     * @throws IllegalArgumentException if messages is null
     */
    public void info(Object... messages) {
        if (logger.isInfoEnabled()) {
            logger.info(buildMessage(messages));
        }
    }
    
    /**
     * Log a throwable and a message with a variable number of arguments at the info level.
     * Only logs if the info level is enabled.
     * 
     * @param throwable
     * @param messages
     * @throws IllegalArgumentException if messages is null
     */
    public void info(Throwable throwable, Object... messages) {
        if (logger.isInfoEnabled()) {
            logger.info(buildMessage(messages), throwable);
        }
    }
    
    /**
     * Log a message with a variable number of arguments at the warn level.
     * Only logs if the warn level is enabled.
     * 
     * @param messages
     * @throws IllegalArgumentException if messages is null
     */
    public void warn(Object... messages) {
        if (logger.isWarnEnabled()) {
            logger.warn(buildMessage(messages));
        }
    }
    
    /**
     * Log a throwable and a message with a variable number of arguments at the warn level.
     * Only logs if the warn level is enabled.
     * 
     * @param throwable
     * @param messages
     * @throws IllegalArgumentException if messages is null
     */
    public void warn(Throwable throwable, Object... messages) {
        if (logger.isWarnEnabled()) {
            logger.warn(buildMessage(messages), throwable);
        }
    }
    
    /**
     * Log a message with a variable number of arguments at the error level.
     * Only logs if the error level is enabled.
     * 
     * @param messages
     * @throws IllegalArgumentException if messages is null
     */
    public void error(Object... messages) {
        if (logger.isErrorEnabled()) {
            logger.error(buildMessage(messages));
        }
    }
    
    /**
     * Log a throwable and a message with a variable number of arguments at the error level.
     * Only logs if the error level is enabled.
     * 
     * @param throwable
     * @param messages
     * @throws IllegalArgumentException if messages is null
     */
    public void error(Throwable throwable, Object... messages) {
        if (logger.isErrorEnabled()) {
            logger.error(buildMessage(messages), throwable);
        }
    }
    
    /**
     * Log a message with a variable number of arguments at the fatal level.
     * Only logs if the fatal level is enabled.
     * 
     * <p>
     * Note: Currently fatal logs at the same level as error. This may change in a future release.
     * 
     * @param messages
     * @throws IllegalArgumentException if messages is null
     */
    public void fatal(Object... messages) {
        error(messages);
    }
    
    /**
     * Log a throwable and a message with a variable number of arguments at the fatal level.
     * Only logs if the fatal level is enabled.
     * 
     * <p>
     * Note: Currently fatal logs at the same level as error. This may change in a future release.
     * 
     * @param throwable
     * @param messages
     * @throws IllegalArgumentException if messages is null
     */
    public void fatal(Throwable throwable, Object... messages) {
        error(throwable, messages);
    }
    
    /**
     * Get the log level for all loggers.
     * Defaults to 'info'.
     * 
     * @return level the log level
     */
    static Level getLevel() {
        return level;
    }
    
    /**
     * Get the log file path for all loggers.
     * Defaults to 'logs'.
     * 
     * @return filePath the log file path
     */
    static String getFilePath() {
        return filePath;
    }
    
    /**
     * Get the log file name for all loggers.
     * Defaults to 'newrelic_plugin.log'.
     * 
     * @return fileName the log file name
     */
    static String getFileName() {
        return fileName;
    }
    
    /**
     * Get the log file limit in kilobytes.
     * Defaults to '25600' or 25 mb.
     * 
     * @return fileLimitInKilobytes the log file limit in kilobytes
     */
    static Integer getFileLimitInKBytes() {
        return fileLimitInKilobytes;
    }
    
    static String buildMessage(Object... messages) {
        if (messages == null) {
            throw new IllegalArgumentException("'messages' cannot be null");
        }
        
        StringBuilder builder = new StringBuilder();
        for (Object message : messages) {
            builder.append(message);
        }
        return builder.toString();
    }
    
    /*
     * Translate supported log level to Logback level
     */
    static ch.qos.logback.classic.Level translateLevel(Level level) {
        if (level == null) {
            throw new IllegalArgumentException("'level' cannot be null");
        }
        
        switch (level) {
        case Debug:
            return ch.qos.logback.classic.Level.DEBUG;
        case Info:
            return ch.qos.logback.classic.Level.INFO;
        case Warn:
            return ch.qos.logback.classic.Level.WARN;
        case Error:
            return ch.qos.logback.classic.Level.ERROR;
        case Fatal:
            return ch.qos.logback.classic.Level.ERROR;
        default:
            return ch.qos.logback.classic.Level.INFO;
        }
    }
}
