package com.newrelic.metrics.publish;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;

import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.binding.Request;

public class AgentTest {

    private static String GUID = "com.test.onecycle";
    private static String VERSION = "1.2.3";

    @Test
    public void testOnePollCycle() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        // agent to test
        OneCycleAgent agent = new OneCycleAgent("TestAgent");

        Context context = new Context();
        context.agentData.version = VERSION;
        agent.getCollector().setContext(context);
        agent.getCollector().createComponent(agent.getGUID(), agent.getAgentName());

        Request request = context.createRequest();

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
        expectedAgent.put("version", VERSION);
        expected.put("agent", expectedAgent);

        // expected components
        LinkedList<HashMap<String, Object>> expectedComponents = new LinkedList<HashMap<String, Object>>();
        HashMap<String, Object> expectedComponent = new HashMap<String, Object>();
        expectedComponent.put("guid", GUID);
        expectedComponent.put("name", "TestAgent");
        expectedComponent.put("duration", 60);

        // expected metrics
        HashMap<String, Object> expectedMetrics = new HashMap<String, Object>();
        expectedMetrics.put("Component/Cycles/Count[cycles]", Arrays.<Number>asList(5.0f, 2, 2.0f, 3.0f, 25.0f));
        expectedComponent.put("metrics", expectedMetrics);

        expectedComponents.add(expectedComponent);
        expected.put("components", expectedComponents);

        assertEquals(expected, serializedRequest);
    }

    @Test
    public void testOnePollCycleWithMultipleComponents() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        Context context = new Context();
        context.agentData.version = VERSION;
        Request request = context.createRequest();

        for(int i = 0; i < 3; i++) {
            // agent to test
            OneCycleAgent agent = new OneCycleAgent("TestAgent" + i);
            agent.getCollector().setContext(context);
            agent.getCollector().createComponent(agent.getGUID(), agent.getAgentName());
            agent.getCollector().setRequest(request);

            // one poll cycle
            agent.pollCycle();
        }

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
        expectedAgent.put("version", VERSION);
        expected.put("agent", expectedAgent);

        // expected components
        LinkedList<HashMap<String, Object>> expectedComponents = new LinkedList<HashMap<String, Object>>();

        for(int i = 0; i < 3; i++) {
            HashMap<String, Object> expectedComponent = new HashMap<String, Object>();
            expectedComponent.put("guid", GUID);
            expectedComponent.put("name", "TestAgent" + i);
            expectedComponent.put("duration", 60);

            // expected metrics
            HashMap<String, Object> expectedMetrics = new HashMap<String, Object>();
            expectedMetrics.put("Component/Cycles/Count[cycles]", Arrays.<Number>asList(5.0f, 2, 2.0f, 3.0f, 25.0f));
            expectedComponent.put("metrics", expectedMetrics);

            expectedComponents.add(expectedComponent);
        }

        expected.put("components", expectedComponents);

        assertEquals(expected, serializedRequest);
    }

    private static class OneCycleAgent extends Agent {

        private String name;

        public OneCycleAgent(String name) {
            super(GUID, VERSION);

            this.name = name;
        }

        @Override
        public void pollCycle() {
            reportMetric("Cycles/Count", "cycles", 2, 5, 2, 3, 25);
        }

        @Override
        public String getAgentName() {
            return name;
        }
    }
}
