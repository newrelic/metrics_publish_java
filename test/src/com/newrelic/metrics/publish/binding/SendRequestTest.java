package com.newrelic.metrics.publish.binding;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.Date;

import org.junit.Test;

public class SendRequestTest {

    @Test
    public void testSendWithComponentDataTimestamps() throws Exception {
        Context context = new OkStatusResponseContext();
        context.agentData.host = "test host";
        context.agentData.pid = 5;
        context.agentData.version = "1.2.3";
        
        ComponentData component = context.createComponent();
        component.guid = "com.test.guid";
        component.name = "test component name";
        
        Request request = new Request(context);
        request.addMetric(component, "test metric", 17.0f);
        
        request.send();
        
        Date lastSuccessfulReportedAt = getLastSuccessfulReportedAt(component);
        assertNotNull(lastSuccessfulReportedAt);
    }
    
    private Date getLastSuccessfulReportedAt(ComponentData component) throws Exception {
        Field lastSuccessfulReportedAt = component.getClass().getDeclaredField("lastSuccessfulReportedAt");
        lastSuccessfulReportedAt.setAccessible(true);
        return (Date) lastSuccessfulReportedAt.get(component);
    }
    
    private static class OkStatusResponseContext extends Context {
        
        @Override
        /* package */ HttpURLConnection createUrlConnectionForOutput() throws IOException {
            
            // mocking response with 200 and ok status
            HttpURLConnection mockConnection = createNiceMock(HttpURLConnection.class);
            
            expect(mockConnection.getOutputStream()).andReturn(new ByteArrayOutputStream(10000));
            expect(mockConnection.getResponseCode()).andReturn(HttpURLConnection.HTTP_OK);
            expect(mockConnection.getInputStream()).andReturn(new ByteArrayInputStream("{\"status\":\"ok\"}".getBytes()));
            
            replay(mockConnection);

            return mockConnection;
        }
    }
}
