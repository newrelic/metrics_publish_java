package com.newrelic.metrics.publish.binding;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Date;

import org.junit.Test;

public class SendRequestTest {

    @Test
    public void testSendWithComponentDataTimestamps() throws Exception {
        Context context = BindingFactory.createContext();
        
        ComponentData component = BindingFactory.createComponent(context);
        
        Request request = context.createRequest();
        request.addMetric(component, "test metric", 17.0f);
        
        request.deliver();
        
        Date lastSuccessfulReportedAt = getLastSuccessfulReportedAt(component);
        assertNotNull(lastSuccessfulReportedAt);
    }
    
    private Date getLastSuccessfulReportedAt(ComponentData component) throws Exception {
        Field lastSuccessfulReportedAt = component.getClass().getDeclaredField("lastSuccessfulReportedAt");
        lastSuccessfulReportedAt.setAccessible(true);
        return (Date) lastSuccessfulReportedAt.get(component);
    }
}
