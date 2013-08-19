package com.newrelic.metrics.publish.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class RequestTest {
    
    @Test
    public void testAddMetric() {
        
        Context context = BindingFactory.createContext();
        ComponentData component = BindingFactory.createComponent(context);
        
        Request request = context.createRequest();
        request.addMetric(component, "test metric", 7.0f);
        
        List<MetricData> metrics = request.getMetrics(component);
        
        assertEquals(1, metrics.size());
        assertTrue(metrics.get(0).name.equals("test metric"));
        assertTrue(metrics.get(0).count == 1);
        assertEquals(7.0f, metrics.get(0).value, 0.00001);
        assertEquals(7.0f, metrics.get(0).minValue, 0.00001);
        assertEquals(7.0f, metrics.get(0).maxValue, 0.00001);
        assertEquals(49.0f, metrics.get(0).sumOfSquares, 0.00001);
    }
    
    @Test
    public void testAddMultipleMetrics() {
        
        Context context = BindingFactory.createContext();
        ComponentData component = BindingFactory.createComponent(context);
        
        Request request = context.createRequest();
        request.addMetric(component, "test metric", 7.0f);
        request.addMetric(component, "test metric2", 10.0f);
        
        List<MetricData> metrics = request.getMetrics(component);
        
        assertEquals(2, metrics.size());
        
        assertTrue(metrics.get(0).name.equals("test metric"));
        assertTrue(metrics.get(0).count == 1);
        assertEquals(7.0f, metrics.get(0).value, 0.00001);
        assertEquals(7.0f, metrics.get(0).minValue, 0.00001);
        assertEquals(7.0f, metrics.get(0).maxValue, 0.00001);
        assertEquals(49.0f, metrics.get(0).sumOfSquares, 0.00001);
        
        assertTrue(metrics.get(1).name.equals("test metric2"));
        assertTrue(metrics.get(1).count == 1);
        assertEquals(10.0f, metrics.get(1).value, 0.00001);
        assertEquals(10.0f, metrics.get(1).minValue, 0.00001);
        assertEquals(10.0f, metrics.get(1).maxValue, 0.00001);
        assertEquals(100.0f, metrics.get(1).sumOfSquares, 0.00001);
    }
    
    @Test
    public void testAddDetailedMetric() {
        
        Context context = BindingFactory.createContext();
        ComponentData component = BindingFactory.createComponent(context);
        
        Request request = context.createRequest();
        request.addMetric(component, "test metric", 2, 12.0f, 2.0f, 10.0f, 144.0f);
        
        List<MetricData> metrics = request.getMetrics(component);
        
        assertEquals(1, metrics.size());
        assertTrue(metrics.get(0).name.equals("test metric"));
        assertTrue(metrics.get(0).count == 2);
        assertEquals(12.0f, metrics.get(0).value, 0.00001);
        assertEquals(2.0f, metrics.get(0).minValue, 0.00001);
        assertEquals(10.0f, metrics.get(0).maxValue, 0.00001);
        assertEquals(144.0f, metrics.get(0).sumOfSquares, 0.00001);
    }
    
    @Test
    public void testMetricAggregation() {
        
        Context context = BindingFactory.createContext();
        ComponentData component = BindingFactory.createComponent(context);
        
        Request request = context.createRequest();
        request.addMetric(component, "test metric", 2.0f);
        request.addMetric(component, "test metric", 4.0f);
        
        List<MetricData> metrics = request.getMetrics(component);
        
        assertEquals(1, metrics.size());
        assertTrue(metrics.get(0).name.equals("test metric"));
        assertTrue(metrics.get(0).count == 2);
        assertEquals(6.0f, metrics.get(0).value, 0.00001);
        assertEquals(2.0f, metrics.get(0).minValue, 0.00001);
        assertEquals(4.0f, metrics.get(0).maxValue, 0.00001);
        assertEquals(20.0f, metrics.get(0).sumOfSquares, 0.00001);
    }
    
    @Test
    public void testDetailedMetricAggregation() {
        
        Context context = BindingFactory.createContext();
        ComponentData component = BindingFactory.createComponent(context);
        
        Request request = context.createRequest();
        request.addMetric(component, "test metric", 3, 20.0f, 5.0f, 10.0f, 400.0f);
        request.addMetric(component, "test metric", 4, 40.0f, 4.0f, 20.0f, 1600.0f);
        
        List<MetricData> metrics = request.getMetrics(component);
        
        assertEquals(1, metrics.size());
        assertTrue(metrics.get(0).name.equals("test metric"));
        assertTrue(metrics.get(0).count == 7);
        assertEquals(60.0f, metrics.get(0).value, 0.00001);
        assertEquals(4.0f, metrics.get(0).minValue, 0.00001);
        assertEquals(20.0f, metrics.get(0).maxValue, 0.00001);
        assertEquals(2000.0f, metrics.get(0).sumOfSquares, 0.00001);
    }
    
    @Test
    public void testSerialize() {
        
        Context context = BindingFactory.createContext();
        ComponentData component = BindingFactory.createComponent(context);

        Request request = context.createRequest();
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
        expectedMetrics.put("test metric", Arrays.<Number>asList(17.0f, 1, 17.0f, 17.0f, 289.0f));
        expectedComponent.put("metrics", expectedMetrics);
        
        expectedComponents.add(expectedComponent);
        expected.put("components", expectedComponents);
        
        assertEquals(expected, data);
    }
    
    @Test
    public void testDelivered() {
        
        Context context = BindingFactory.createContext();
        ComponentData component = BindingFactory.createComponent(context);

        Request request = context.createRequest();
        request.addMetric(component, "test metric", 17.0f);
        
        request.deliver();
        
        assertTrue(request.isDelivered());
    }
    
    @Test
    public void testNotDelivered() {
        
        Context context = BindingFactory.createContextWithUnavailableResponse();
        ComponentData component = BindingFactory.createComponent(context);

        Request request = context.createRequest();
        request.addMetric(component, "test metric", 17.0f);
        
        request.deliver();
        
        assertFalse(request.isDelivered());
    }
}
