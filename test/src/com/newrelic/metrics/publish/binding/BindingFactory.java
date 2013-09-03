package com.newrelic.metrics.publish.binding;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;

public class BindingFactory {

    public static Context createContext() {
        Context context = new MockContext();
        context.agentData.host = "test host";
        context.agentData.pid = 5;
        context.agentData.version = "1.2.3";
        return context;
    }
    
    public static Context createContextWithUnavailableResponse() {
        return createContext(HttpURLConnection.HTTP_UNAVAILABLE, "{\"status\":\"ok\"}");
    }
    
    public static Context createContextWithBadResponse() {
        return createContext(HttpURLConnection.HTTP_BAD_REQUEST, "{\"status\":\"error\"}");
    }
    
    public static Context createContext(int responseCode, String responseBody) {
        Context context = new MockContext(responseCode, responseBody);
        context.agentData.host = "test host";
        context.agentData.pid = 5;
        context.agentData.version = "1.2.3";
        return context;
    }
    
    public static ComponentData createComponent(Context context) {
        ComponentData component = context.createComponent();
        component.guid = "com.test.guid";
        component.name = "test component name";
        return component;
    }
    
    public static Request createRequest(Context context) {
        return context.createRequest();
    }
    
    public static class MockContext extends Context {
        
        private final int responseCode;
        private final String responseBody;
        
        public MockContext() {
            this(HttpURLConnection.HTTP_OK, "{\"status\":\"ok\"}");
        }
        
        public MockContext(int responseCode, String responseBody) {
            this.responseCode = responseCode;
            this.responseBody = responseBody;
        }
        
        @Override
        /* package */ HttpURLConnection createUrlConnectionForOutput() throws IOException {
            
            // mocking response with responseCode and responseBody
            HttpURLConnection mockConnection = createNiceMock(HttpURLConnection.class);
            
            expect(mockConnection.getOutputStream()).andReturn(new ByteArrayOutputStream(10000));
            expect(mockConnection.getResponseCode()).andReturn(responseCode);
            if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                expect(mockConnection.getInputStream()).andReturn(new ByteArrayInputStream(responseBody.getBytes()));
            } else {
                expect(mockConnection.getErrorStream()).andReturn(new ByteArrayInputStream(responseBody.getBytes()));
            }
            
            replay(mockConnection);

            return mockConnection;
        }
    }
}
