package com.newrelic.metrics.publish.binding;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.newrelic.metrics.publish.configuration.Config;
import com.newrelic.metrics.publish.util.Logger;

/**
 * Provisional API which is subject to change.
 * The context for a {@link Request} that manages {@link AgentData} and {@link ComponentData}.
 */
public class Context {
    
    private static final Logger logger = Logger.getLogger(Context.class);

    private static final String SERVICE_URI = "https://platform-api.newrelic.com/platform/v1/metrics";
    
    private static final String POST = "POST";
    private static final String X_LICENSE_KEY = "X-License-Key";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String ACCEPT = "Accept";
    private static final String AGENT = "agent";
    private static final String COMPONENTS = "components";
    private static final String USER_AGENT = "User-Agent";

    private static final long AGGREGATION_LIMIT = TimeUnit.MINUTES.toMillis(20);
    private static final int CONNECTION_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(20);
    
    public String licenseKey;
    public AgentData agentData;

    private String serviceURI = SERVICE_URI;
    private boolean sslHostVerification = true;
    private LinkedList<ComponentData> components;

    private Request lastRequest;
    private Date aggregationStartedAt;

    /**
     * Constructs a {@code Context}
     */
    public Context() {
        super();
        agentData = new AgentData();
        components = new LinkedList<ComponentData>();
        lastRequest = new Request(this);
        aggregationStartedAt = new Date();
    }

    /**
     * Create a {@link Request}.
     * If the last {@code Request} was not sent successfully, the last {@code Request} will be reused.
     * This guarantees that previously reported metrics will be aggregated with new metrics, and
     * no metric data will be lost if a request was not sent successfully.
     * @return request
     */
    public Request createRequest() {
        if (isPastAggregationLimit()) {
            lastRequest = new Request(this);
            for (ComponentData component : components) {
                component.setLastSuccessfulReportedAt(null);
            }
        }
        else if (isLastRequestDelivered()) {
            lastRequest = new Request(this);
        }
        return lastRequest;
    }

    private boolean isLastRequestDelivered() {
        return lastRequest.isDelivered();
    }

    /* package */ boolean isPastAggregationLimit() {
        if (aggregationStartedAt != null) {
            long aggregationDuration = new Date().getTime() - aggregationStartedAt.getTime();
            return aggregationDuration > AGGREGATION_LIMIT;
        }
        return false;
    }

    /* package */ void setAggregationStartedAt(Date aggregationStartedAt) {
        this.aggregationStartedAt = aggregationStartedAt;
    }

    /* package */ Date getAggregationStartedAt() {
        return aggregationStartedAt;
    }

    /**
     * Create a {@link ComponentData} that reported metrics will belong to.
     * @return ComponentData
     */
    public ComponentData createComponent() {
        ComponentData componentData = new ComponentData();
        add(componentData);
        return componentData;
    }

    /**
     * Get an {@link Iterator} for the list of {@link ComponentData}
     * @return Iterator
     */
    public Iterator<ComponentData> getComponents() {
        return components.iterator();
    }

    /**
     * Return the URI of the metric data service that metric data gets posted to.
     */
    public String getServiceURI() {
        return serviceURI;
    }

    /**
     * An internal method for debug purposes only, not for general usage by clients of the SDK
     */
    public void internalSetServiceURI(String URI) {
        serviceURI = URI;
    }

    /**
     * Internal method for setting ssl host verification
     * @param sslHostVerification
     */
    public void internalSetSSLHostVerification(boolean sslHostVerification) {
        this.sslHostVerification = sslHostVerification;
    }

    /* package */ void add(ComponentData componentData) {
        components.add(componentData);
    }

    /**
     * Create an http url connection to post data to the New Relic service.
     *
     * @return HttpURLConnection
     * @throws IOException
     */
    /* package */ HttpURLConnection createUrlConnectionForOutput() throws IOException {
        URL serviceUrl = new URL(serviceURI);
        
        logger.debug("Metric service url: ", serviceUrl);

        HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection();
        connection.setRequestMethod(POST);
        connection.addRequestProperty(X_LICENSE_KEY, licenseKey);
        connection.addRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
        connection.addRequestProperty(ACCEPT, APPLICATION_JSON);
        connection.setConnectTimeout(CONNECTION_TIMEOUT);
        connection.setReadTimeout(CONNECTION_TIMEOUT);
        connection.addRequestProperty(USER_AGENT, getUserAgentString());

        // if not verifying ssl host and using https, add custom hostname verifier
        // else use default hostname verifier
        if (connection instanceof HttpsURLConnection && !sslHostVerification) {
            // ssl hostname verifier verifies any host
            ((HttpsURLConnection) connection).setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }

        connection.setDoOutput(true);
        return connection;
    }

    /* package */ Map<String, Object> serialize(Request request) {
        Map<String, Object> output = new HashMap<String, Object>();
        output.put(AGENT, agentData.serialize());

        LinkedList<HashMap<String, Object>> componentsOutput = new LinkedList<HashMap<String, Object>>();
        output.put(COMPONENTS, componentsOutput);

        for (ComponentData component : components) {
            HashMap<String, Object> map = component.serialize(request);

            if(!map.isEmpty()) {
                componentsOutput.add(map);
            }
        }

        return output;
    }
    
    /* package */ String getUserAgentString() {
    	return String.format("JavaSDK/%s (%s %s)", Config.getSdkVersion(), System.getProperty("os.name"), System.getProperty("os.version"));
    }
}
