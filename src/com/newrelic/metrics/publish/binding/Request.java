package com.newrelic.metrics.publish.binding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Request {
	
    private static final String EMPTY_STRING = "";
    private static final String STATUS = "status";
    private static final String OK_STATUS = "ok";
	
	private final Context context;
	private final HashMap<ComponentData, LinkedList<MetricData>> metrics = new HashMap<ComponentData, LinkedList<MetricData>>(); 
	private final int duration;
	
	public Request(Context context, int duration) {
		super();
		this.context = context;
		this.duration = duration;
	}
	
	public int getDuration() {
		return duration;
	}	

	public MetricData addMetric(ComponentData component, String name, int value) {
		return addMetric(component, new MetricData(component, name, value));
	}
	
	public MetricData addMetric(ComponentData component, String name, float value) {
		return addMetric(component, new MetricData(component, name, value));
	}
	
    public void send() {
        HttpURLConnection connection = null;
        Logger logger = Context.getLogger();
        
        try {
            connection = context.createUrlConnectionForOutput();
            
            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
            try {
            	Map<String, Object> data = serialize();
            	
            	String json = JSONObject.toJSONString(data);
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("Sending JSON: " + json);
                }
            	
                out.write(json);
            } finally {
                out.close();
            }
            
            // process and log response from the collector
            processResponse(connection);
        } 
        catch (Exception ex) 
        {
            logger.severe("An error occurred communicating with the New Relic service - " + ex.getMessage());
            logger.log(Level.FINE, ex.getMessage(), ex);

            if (connection != null) {
                try {
                   logger.info("Response: " + connection.getResponseCode() + " : " + connection.getResponseMessage() );
                 } catch (IOException e) {
                    logger.log(Level.FINER, ex.getMessage(), ex);
                }
            }
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Process response and log response as appropriate
     * @param connection
     * @throws IOException
     */
    private void processResponse(HttpURLConnection connection) throws IOException {  	
    	int responseCode = connection.getResponseCode();
  
        // do not log 503 responses
        if (responseCode == 503) {
        	Context.getLogger().info("Collector temporarily unavailable...continuing");
        } else {
        	// read server response
        	String responseBody = getServerResponse(connection.getInputStream());
        	if (responseBody == null || EMPTY_STRING.equals(responseBody)) {
        		Context.getLogger().info("Failed server response: no response");
        	} else { 
        		// parse json response for status message
        		String statusMessage = getStatusMessage(responseBody);
        		if (responseCode == 200 && OK_STATUS.equals(statusMessage)) {
        			Context.getLogger().info("Server response: " + responseCode + ", " + responseBody);
        		} else {
        			// all other response codes will fail
        			Context.getLogger().info("Failed server response: " + responseCode + ", " + responseBody);
        		}
        	}
        }
    }
    
    /**
     * Get server response from provided input stream
     * @param input
     * @return String the server response
     * @throws IOException
     */
    private String getServerResponse(InputStream input) throws IOException {
    	StringBuilder builder = new StringBuilder();
    	BufferedReader in = new BufferedReader(new InputStreamReader(input));
		try {
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				builder.append(inputLine);
			}
		} finally {
			in.close();
		}
    	return builder.toString();
    }
    
    /**
     * Get status message by parsing JSON response body.
     * Will return null if no status is present.
     * @param responseBody
     * @return String the status message
     */
    private String getStatusMessage(String responseBody) {
    	Object jsonObj = JSONValue.parse(responseBody);
    	JSONObject json = (JSONObject) jsonObj;
    	Object status = json.get(STATUS);
    	if (status != null) {
    		return (String) status;
    	} else {
    		return null;
    	}
    }
    
	/* package */ Map<String, Object> serialize() {		
		return context.serialize(this);
	}	

	/* package */ LinkedList<MetricData> getMetrics(ComponentData component) {
		if( ! metrics.containsKey(component)) {
			metrics.put(component, new LinkedList<MetricData>() );
		}
		return metrics.get(component);
	}
	
	private MetricData addMetric(ComponentData component, MetricData metric) {
		Context.getLogger().finest(component.guid + " " + metric.name + ":" + metric.value);
		getMetrics(component).add(metric);
		return metric;
	}
}
