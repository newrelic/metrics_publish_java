package com.newrelic.metrics.publish.binding;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;

public class AgentDataTest {

    @Test
    public void testSerialize() {
        
        // test object
        AgentData agent = new AgentData();
        agent.host = "test agent";
        agent.version = "1.0.0";
        agent.pid = 10;
        
        // test method
        HashMap<String, Object> data = agent.serialize();
        
        // expected response
        HashMap<String, Object> expected = new HashMap<String, Object>();
        expected.put("host", "test agent");
        expected.put("version", "1.0.0");
        expected.put("pid", 10);
        
        assertEquals(expected, data);
    }
}
