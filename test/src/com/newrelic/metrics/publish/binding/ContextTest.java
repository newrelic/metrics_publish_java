package com.newrelic.metrics.publish.binding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.newrelic.metrics.publish.configuration.Config;

public class ContextTest {

    @Test
    public void testCreateComponent() {
        
        Context context = new Context();
        ComponentData firstComponent = context.createComponent();
        firstComponent.guid = "com.test.guid.first";
        firstComponent.name = "first test component name";
        
        ComponentData secondComponent = context.createComponent();
        secondComponent.guid = "com.test.guid";
        secondComponent.name = "test component name";
        
        // test that only 2 components are added in the correct order
        int size = 0;
        ComponentData itrComponent = null; 
        Iterator<ComponentData> itr = context.getComponents();
        while (itr.hasNext()) {
            ++size;
            itrComponent = itr.next();
        }
        
        assertTrue(size == 2);
        assertTrue(itrComponent != null);
        assertEquals("com.test.guid", itrComponent.guid);
        assertEquals("test component name", itrComponent.name);
    }
    
    @Test
    public void testCreateRequest() {
        
        Context context = BindingFactory.createContext();
        Request request = context.createRequest();
        
        assertNotNull(request);
    }
    
    @Test
    public void testFailedRequestIsReused() {
        
        Context context = BindingFactory.createContextWithUnavailableResponse();
        Request request = context.createRequest();
        request.addMetric(BindingFactory.createComponent(context), "test metric", 1.0f);
        
        request.deliver();
        
        Request duplicate = context.createRequest();
        
        assertSame(request, duplicate);
    }
    
    @Test
    public void testUnavailableRequestDoesNotUpdateAggregatedTimestamp() throws InterruptedException {
        Context context = BindingFactory.createContextWithUnavailableResponse();
        Request request = context.createRequest();
        Date start = context.getAggregationStartedAt();
        request.addMetric(BindingFactory.createComponent(context), "test metric", 1.0f);
        
        // need to wait for at least one millisecond for timestamp resolution
        TimeUnit.MILLISECONDS.sleep(1);
        request.deliver();
        
        // failed request did not update timestamp
        assertEquals(start, context.getAggregationStartedAt());
    }
    
    @Test
    public void testErrorRequestDoesNotUpdateAggregatedTimestamp() throws InterruptedException {
        Context context = BindingFactory.createContextWithBadResponse();
        Request request = context.createRequest();
        Date start = context.getAggregationStartedAt();
        request.addMetric(BindingFactory.createComponent(context), "test metric", 1.0f);
        
        // need to wait for at least one millisecond for timestamp resolution
        TimeUnit.MILLISECONDS.sleep(1);
        request.deliver();
        
        // failed request did not update timestamp
        assertEquals(start, context.getAggregationStartedAt());
    }
    
    @Test
    public void testSuccessfulRequestUpdatesAggregatedTimestamp() throws InterruptedException {
        Context context = BindingFactory.createContext();
        Request request = context.createRequest();
        Date start = context.getAggregationStartedAt();
        request.addMetric(BindingFactory.createComponent(context), "test metric", 1.0f);
        
        // need to wait for at least one millisecond for timestamp resolution
        TimeUnit.MILLISECONDS.sleep(1);
        request.deliver();
        
        // successful request should update timestamp
        assertTrue(start.before(context.getAggregationStartedAt()));
    }
    
    @Test
    public void testDefaultAggregationLimit() {
        Context context = BindingFactory.createContext();
        assertFalse(context.isPastAggregationLimit());
    }
    
    @Test
    public void testUnderAggregationLimit() {
        Context context = BindingFactory.createContext();        
        Date nineteenMinutesAgo = new Date(new Date().getTime() - TimeUnit.MINUTES.toMillis(19));
        context.setAggregationStartedAt(nineteenMinutesAgo);
        assertFalse(context.isPastAggregationLimit());
    }
    
    @Test
    public void testOverAggregationLimit() {
        Context context = BindingFactory.createContext();        
        Date twentyOneMinutesAgo = new Date(new Date().getTime() - TimeUnit.MINUTES.toMillis(21));
        context.setAggregationStartedAt(twentyOneMinutesAgo);
        assertTrue(context.isPastAggregationLimit());
    }
    
    @Test
    public void testOverAggregationLimitCreatesNewRequest() {
        Context context = BindingFactory.createContext();
        Request request = context.createRequest();
        Date twentyOneMinutesAgo = new Date(new Date().getTime() - TimeUnit.MINUTES.toMillis(21));
        context.setAggregationStartedAt(twentyOneMinutesAgo);
        Request newRequest = context.createRequest();
        
        assertNotSame(request, newRequest);
    }
    
    @Test
    public void testGetUserAgent() {
    	Context context = BindingFactory.createContext();
    	String userAgent = context.getUserAgentString();
    	String sdkVersion = Config.getSdkVersion();
    	
    	assertNotNull(userAgent);
    	assertTrue(userAgent.contains(sdkVersion));
    }
}
