package com.newrelic.metrics.publish;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;

import com.newrelic.metrics.publish.binding.Request;

public class AgentTest {

    @Test
    public void testOnePollCycle() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        // agent to test
        OneCycleAgent agent = new OneCycleAgent("com.test.onecycle", "1.2.3");
        agent.prepareToRun();
        
        Request request = new Request(agent.getCollector().getContext(), 60);
        agent.getCollector().setRequest(request);
        
        // one poll cycle
        agent.pollCycle();
        
        // serialize request
        Method serializeMethod = request.getClass().getDeclaredMethod("serialize");
        serializeMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> serializedRequest = (Map<String, Object>) serializeMethod.invoke(request);
        
        // expected outcome
        HashMap<String, Object> expected = new HashMap<String, Object>();
        
        // expected agent
        HashMap<String, Object> expectedAgent = new HashMap<String, Object>();
        expectedAgent.put("host", "host");
        expectedAgent.put("pid", 0);
        expectedAgent.put("version", "1.2.3");
        expected.put("agent", expectedAgent);
        
        // expected components
        LinkedList<HashMap<String, Object>> expectedComponents = new LinkedList<HashMap<String, Object>>();
        HashMap<String, Object> expectedComponent = new HashMap<String, Object>();
        expectedComponent.put("guid", "com.test.onecycle");
        expectedComponent.put("name", "One Cycle Agent");
        expectedComponent.put("duration", 60);
        
        // expected metrics
        HashMap<String, Object> expectedMetrics = new HashMap<String, Object>();
        expectedMetrics.put("Component/Cycles/Count[cycles]", Arrays.<Number>asList(5.0f, 2, 2.0f, 3.0f, 25.0f));
        expectedComponent.put("metrics", expectedMetrics);
        
        expectedComponents.add(expectedComponent);
        expected.put("components", expectedComponents);
        
        assertEquals(expected, serializedRequest);
    }
    
    private static class OneCycleAgent extends Agent {

        public OneCycleAgent(String GUID, String version) {
            super(GUID, version);
        }

        @Override
        public void pollCycle() {
            reportMetric("Cycles/Count", "cycles", 2, 5, 2, 3, 25);
        }

        @Override
        public String getComponentHumanLabel() {
            return "One Cycle Agent";
        }
    }
}
