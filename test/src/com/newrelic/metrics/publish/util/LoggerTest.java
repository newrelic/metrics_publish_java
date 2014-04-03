package com.newrelic.metrics.publish.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.newrelic.metrics.publish.util.Logger;
import com.newrelic.metrics.publish.util.Logger.Level;

public class LoggerTest {
    
    @Test
    public void testLoggerInit() {
        Logger.init("debug", "/var/log/newrelic", "newrelic.log", 1024);
        
        assertEquals(Level.Debug, Logger.getLevel());
        assertEquals("/var/log/newrelic", Logger.getFilePath());
        assertEquals("newrelic.log", Logger.getFileName());
        assertEquals(1024, Logger.getFileLimitInKBytes().intValue());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testLoggerInitLevelIsNull() {
        Logger.init(null, "/var/log/newrelic", "newrelic.log", 1024);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testLoggerInitLevelIsEmptyString() {
        Logger.init("", "/var/log/newrelic", "newrelic.log", 1024);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testLoggerInitLogFilePathIsNull() {
        Logger.init("debug", null, "newrelic.log", 1024);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testLoggerInitLogFilePathIsEmptyString() {
        Logger.init("debug", "", "newrelic.log", 1024);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testLoggerInitLogFileNameIsNull() {
        Logger.init("debug", "/var/log/newrelic", null, 1024);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testLoggerInitLogFileNameIsEmptyString() {
        Logger.init("debug", "/var/log/newrelic", "", 1024);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testLoggerInitLogLimitInKBytesIsNull() {
        Logger.init("debug", "/var/log/newrelic", "newrelic.log", null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testLoggerInitLogLimitInKBytesIsNegative() {
        Logger.init("debug", "/var/log/newrelic", "newrelic.log", -1);
    }
    
    @Test
    public void testLevelFromString() {
        assertEquals(Level.Debug, Level.fromString("debug"));
        assertEquals(Level.Info,  Level.fromString("info"));
        assertEquals(Level.Warn,  Level.fromString("warn"));
        assertEquals(Level.Error, Level.fromString("error"));
        assertEquals(Level.Fatal, Level.fromString("fatal"));
    }
    
    @Test
    public void testLevelFromStringIsCaseInsensitive() {
        assertEquals(Level.Debug, Level.fromString("DeBug"));
        assertEquals(Level.Info,  Level.fromString("INFO"));
        assertEquals(Level.Warn,  Level.fromString("Warn"));
        assertEquals(Level.Error, Level.fromString("ERror"));
        assertEquals(Level.Fatal, Level.fromString("fATAL"));
    }
    
    @Test
    public void testLevelFromStringWithInvalidLevel() {
        assertNull(Level.fromString("invalid_level"));
        assertNull(Level.fromString(""));
        assertNull(Level.fromString(null));
    }
    
    @Test
    public void testTranslateLevel() {
        assertEquals(ch.qos.logback.classic.Level.DEBUG, Logger.translateLevel(Level.Debug));
        assertEquals(ch.qos.logback.classic.Level.INFO,  Logger.translateLevel(Level.Info));
        assertEquals(ch.qos.logback.classic.Level.WARN,  Logger.translateLevel(Level.Warn));
        assertEquals(ch.qos.logback.classic.Level.ERROR, Logger.translateLevel(Level.Error));
        assertEquals(ch.qos.logback.classic.Level.ERROR, Logger.translateLevel(Level.Fatal));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testTranslateLevelThrowsIllegalArgumentExceptionOnNull() {
        Logger.translateLevel(null);
    }
    
    @Test
    public void testBuildMessage() {
        assertEquals("", Logger.buildMessage(""));
        assertEquals("test message", Logger.buildMessage("test message"));
        assertEquals("test message", Logger.buildMessage("test ", "message"));
        assertEquals("test message\t", Logger.buildMessage("test ", "message", "\t"));
    }
    
    @Test
    public void testBuildMessageCallsToStringOnObjects() {
        Object testObject = new Object() {
            public String toString() {
                return "test message";
            }
        };
        assertEquals("test message", Logger.buildMessage(testObject));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testBuildMessageThrowsIllegalArgumentExceptionOnNull() {
        Logger.buildMessage(null);
    }
}
