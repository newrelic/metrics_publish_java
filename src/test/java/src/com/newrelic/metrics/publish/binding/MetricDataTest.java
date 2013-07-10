package com.newrelic.metrics.publish.binding;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;

public class MetricDataTest {
    
    @Test
    public void testSerialize() {       
        MetricData metric = new MetricData("test metric", 10);
        
        HashMap<String, Object> data = new HashMap<String, Object>();
        metric.serialize(data);
        
        HashMap<String, Object> expected = new HashMap<String, Object>();
        expected.put("test metric", 10);
        
        assertTrue(expected.equals(data));
    }
}
