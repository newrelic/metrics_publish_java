package com.newrelic.metrics.publish.binding;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;

public class RequestTest {
    
    @Test
    public void testSerialize() {
        
        // context
        Context context = new Context();
        context.agentData.host = "test host";
        context.agentData.pid = 5;
        context.agentData.version = "1.2.3";
        
        // component
        ComponentData component = context.createComponent();
        component.guid = "com.test.guid";
        component.name = "test component name";
        
        // request
        Request request = new Request(context, 60);
        request.addMetric(component, "test metric", 17.0f);
        
        Map<String, Object> data = request.serialize();
        
        // expected outcome
        HashMap<String, Object> expected = new HashMap<String, Object>();
        
        // expected agent
        HashMap<String, Object> expectedAgent = new HashMap<String, Object>();
        expectedAgent.put("host", "test host");
        expectedAgent.put("pid", 5);
        expectedAgent.put("version", "1.2.3");
        expected.put("agent", expectedAgent);
        
        // expected components
        LinkedList<HashMap<String, Object>> expectedComponents = new LinkedList<HashMap<String, Object>>();
        HashMap<String, Object> expectedComponent = new HashMap<String, Object>();
        expectedComponent.put("guid", "com.test.guid");
        expectedComponent.put("name", "test component name");
        expectedComponent.put("duration", 60);
        
        // expected metrics
        HashMap<String, Object> expectedMetrics = new HashMap<String, Object>();
        expectedMetrics.put("test metric", 17.0f);
        expectedComponent.put("metrics", expectedMetrics);
        
        expectedComponents.add(expectedComponent);
        expected.put("components", expectedComponents);
        
        assertTrue(expected.equals(data));
    }
}
