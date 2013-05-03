package com.newrelic.platform.metrics.publish.binding;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

public class Context {

	private static final String SERVICE_URI = "https://platform-api.newrelic.com/platform/v1/metrics";
	
	public String licenseKey;
	public AgentData agentData;
	
	private String serviceURI = SERVICE_URI;
    private static Logger LOGGER;	
	private LinkedList<ComponentData> components;
	
	public static Logger getLogger() {
		if(LOGGER == null) {
			setLogger(Logger.getAnonymousLogger());
		}
		return LOGGER;
	}
	
	public static void setLogger(Logger logger) {
		LOGGER = logger;
	}
	
	public Context() {
		super();
		agentData = new AgentData();
		components = new LinkedList<ComponentData>();
	}
	
	public ComponentData createComponent() {
		ComponentData componentData = new ComponentData(this);
		return componentData;
	}

	public Iterator<ComponentData> getComponents() {
		return components.iterator();
	}
	
	/**
	 * Return the URI of the metric data service that metric data gets posted to.
	 */
	public String getServiceURI() {
		return serviceURI;
	}
	
	/*
	 * For debug purposes only, not for general usage by clients of the SDK
	 */
	public void internalSetServiceURI(String URI) {
		serviceURI = URI;
	}
	
	protected void add(ComponentData componentData) {
		components.add(componentData);
	}
	
    /*
     * Create an http url connection to post data to the New Relic service.
     *  
     * @return
     * @throws IOException
     */
    protected HttpURLConnection createUrlConnectionForOutput() throws IOException {
        URL serviceUrl = new URL(serviceURI);
        LOGGER.fine("Metric service url: " + serviceUrl);
        
        HttpURLConnection connection = (HttpURLConnection) serviceUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.addRequestProperty("X-License-Key", licenseKey);
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("Accept", "application/json");
        
        connection.setDoOutput(true);
        return connection;
    }
	
	protected Map<String, Object> serialize(Request request) {		
        Map<String, Object> output = new HashMap<String, Object>();
        output.put("agent", agentData.serialize());
        		
		LinkedList<HashMap<String, Object>> componentsOutput = new LinkedList<HashMap<String, Object>>();
		output.put("components", componentsOutput);
		
		for (ComponentData component : components) {
			componentsOutput.add(component.serialize(request));
		}
		
		return output;
	}	

}