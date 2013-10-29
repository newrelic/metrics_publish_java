package com.newrelic.metrics.publish.binding;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import org.junit.Test;

public class ComponentDataTest {

    @Test
    public void testSerialize() {

        Context context = BindingFactory.createContext();
        Request request = context.createRequest();

        // component to test
        ComponentData component = new ComponentData();
        component.guid = "com.test.guid";
        component.name = "test component";

        // test metrics
        request.addMetric(component, "test metric", 7.0f);
        request.addMetric(component, "second test metric", 33.2f);

        // serialize test
        HashMap<String, Object> data = component.serialize(request);

        // expected outcome
        HashMap<String, Object> expected = new HashMap<String, Object>();
        expected.put("guid", "com.test.guid");
        expected.put("name", "test component");
        expected.put("duration", 60);

        HashMap<String, Object> expectedMetrics = new HashMap<String, Object>();
        expectedMetrics.put("test metric", Arrays.<Number>asList(7.0f, 1, 7.0f, 7.0f, 49.0f));
        expectedMetrics.put("second test metric", Arrays.<Number>asList(33.2f, 1, 33.2f, 33.2f, 1102.24f));
        expected.put("metrics", expectedMetrics);

        assertEquals(expected, data);
    }

    @Test
    public void testSerializeComponentWithNoMetrics() {

        Context context = BindingFactory.createContext();
        Request request = context.createRequest();

        // component to test
        ComponentData component = new ComponentData();
        component.guid = "com.test.guid";
        component.name = "test component";

        // serialize test
        HashMap<String, Object> data = component.serialize(request);

        // expected outcome
        HashMap<String, Object> expected = new HashMap<String, Object>();

        assertEquals(expected, data);
    }

    @Test
    public void testDefaultDuration() {

        Context context = BindingFactory.createContext();
        Request request = context.createRequest();

        // component to test
        ComponentData component = new ComponentData();
        component.guid = "com.test.guid";
        component.name = "test component";

        // test metrics
        request.addMetric(component, "test metric", 10);

        // serialize test
        HashMap<String, Object> data = component.serialize(request);

        // expected outcome
        HashMap<String, Object> expected = new HashMap<String, Object>();
        expected.put("guid", "com.test.guid");
        expected.put("name", "test component");
        expected.put("duration", 60);

        HashMap<String, Object> expectedMetrics = new HashMap<String, Object>();
        expectedMetrics.put("test metric", Arrays.<Number>asList(10.0f, 1, 10.0f, 10.0f, 100.0f));
        expected.put("metrics", expectedMetrics);

        assertEquals(expected, data);
    }

    @Test
    public void testCalculatedDuration() {
        Context context = BindingFactory.createContext();
        Request request = BindingFactory.createRequest(context);

        // component to test
        ComponentData component = new ComponentData();
        component.guid = "com.test.guid";
        component.name = "test component";

        // set last successful timestamp to 30 seconds ago
        Date now = new Date();
        component.setLastSuccessfulReportedAt(new Date(now.getTime() - 30000));

        // test metrics
        request.addMetric(component, "test metric", 10);

        // serialize test
        HashMap<String, Object> data = component.serialize(request);

        // expected outcome
        HashMap<String, Object> expected = new HashMap<String, Object>();
        expected.put("guid", "com.test.guid");
        expected.put("name", "test component");
        expected.put("duration", 30);

        HashMap<String, Object> expectedMetrics = new HashMap<String, Object>();
        expectedMetrics.put("test metric", Arrays.<Number>asList(10.0f, 1, 10.0f, 10.0f, 100.0f));
        expected.put("metrics", expectedMetrics);

        assertEquals(expected, data);
    }

    @Test
    public void testRoundedCalculatedDuration() {
        Context context = BindingFactory.createContext();
        Request request = BindingFactory.createRequest(context);

        // component to test
        ComponentData component = new ComponentData();
        component.guid = "com.test.guid";
        component.name = "test component";

        // set last successful timestamp to 30.4 seconds ago
        Date now = new Date();
        component.setLastSuccessfulReportedAt(new Date(now.getTime() - 30400));

        // test metrics
        request.addMetric(component, "test metric", 10);

        // serialize test
        HashMap<String, Object> data = component.serialize(request);

        // expected outcome
        HashMap<String, Object> expected = new HashMap<String, Object>();
        expected.put("guid", "com.test.guid");
        expected.put("name", "test component");
        expected.put("duration", 31);

        HashMap<String, Object> expectedMetrics = new HashMap<String, Object>();
        expectedMetrics.put("test metric", Arrays.<Number>asList(10.0f, 1, 10.0f, 10.0f, 100.0f));
        expected.put("metrics", expectedMetrics);

        assertEquals(expected, data);
    }
}
