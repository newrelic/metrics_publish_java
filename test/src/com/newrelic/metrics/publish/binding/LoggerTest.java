package com.newrelic.metrics.publish.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

public class LoggerTest {

    @Test
    public void testInitialLogLevelWithNoHandlers() {
        Logger logger = Logger.getAnonymousLogger();
        
        assertEquals(Level.INFO, Context.getInitialLogLevel(logger.getHandlers()));
    }
    
    @Test
    public void testInitialLogLevelWithHandlers() throws SecurityException, IOException {
        Logger logger = Logger.getAnonymousLogger();
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINE);
        logger.addHandler(consoleHandler);
        Handler fileHandler = new FileHandler();
        fileHandler.setLevel(Level.FINEST);
        logger.addHandler(fileHandler);
        Context.setLogger(logger);
        
        assertEquals(Level.FINEST, Context.getInitialLogLevel(logger.getHandlers()));
    }
    
    @Test
    public void testInitialLogLevelWithOneAllHandler() throws SecurityException, IOException {
        Logger logger = Logger.getAnonymousLogger();
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINE);
        logger.addHandler(consoleHandler);
        Handler fileHandler = new FileHandler();
        fileHandler.setLevel(Level.ALL);
        logger.addHandler(fileHandler);
        Context.setLogger(logger);
        
        assertEquals(Level.ALL, Context.getInitialLogLevel(logger.getHandlers()));
    }
    
    @Test
    public void testInitialLogLevelWithOneOffHandler() throws SecurityException, IOException {
        Logger logger = Logger.getAnonymousLogger();
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.OFF);
        logger.addHandler(consoleHandler);
        Handler fileHandler = new FileHandler();
        fileHandler.setLevel(Level.FINE);
        logger.addHandler(fileHandler);
        Context.setLogger(logger);
        
        assertEquals(Level.FINE, Context.getInitialLogLevel(logger.getHandlers()));
    }
    
    @Test
    public void testIsLoggable() {
        Logger logger = Context.getLogger();
        
        assertTrue(logger.isLoggable(Level.INFO));
    }
    
    @Test
    public void testIsLoggableFine() throws SecurityException, IOException {
        Logger logger = Logger.getLogger("com.newrelic.metrics.publish");
        
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.OFF);
        logger.addHandler(consoleHandler);
        Handler fileHandler = new FileHandler();
        fileHandler.setLevel(Level.FINE);
        logger.addHandler(fileHandler);
        
        assertTrue(Context.getLogger().isLoggable(Level.FINE));
    }
}
