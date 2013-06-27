package com.newrelic.metrics.publish.binding;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;

public class ComponentDataTest {
    
    @Test
    public void testSerialize() {
        
        Context context = new Context();
        Request request = new Request(context, 60);
        
        // component to test
        ComponentData component = new ComponentData();
        component.guid = "com.test.guid";
        component.name = "test component";
        
        // test metric
        request.addMetric(component, "test metric", 7);
        
        // serialize test
        HashMap<String, Object> data = component.serialize(request);
        
        assertFalse(data.isEmpty());
        
        // expected outcome
        HashMap<String, Object> expected = new HashMap<String, Object>();
        expected.put("guid", "com.test.guid");
        expected.put("name", "test component");
        expected.put("duration", 60);

        HashMap<String, Object> expectedMetrics = new HashMap<String, Object>();
        expectedMetrics.put("test metric", 7);
        expected.put("metrics", expectedMetrics);
        
        assertTrue(expected.equals(data));
    }
}
