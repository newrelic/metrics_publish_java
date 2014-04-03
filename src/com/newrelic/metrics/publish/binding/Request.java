package com.newrelic.metrics.publish.binding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.newrelic.metrics.publish.util.Logger;

/**
 * Provisional API which is subject to change.
 * Represents a request to the New Relic metrics API.
 * 
 * <p> A {@code Request} has an associated {@link Context} and a list of metrics for a given component.
 * The {@code Request} is sent in the JSON format.
 *
 */
public class Request {

    private static final Logger logger = Logger.getLogger(Request.class);
    
    private static final String EMPTY_STRING = "";
    private static final String STATUS = "status";
    private static final String OK_STATUS = "ok";
    private static final String DISABLE_NEW_RELIC = "DISABLE_NEW_RELIC";
    private static final int EXIT_CODE = 1;

    private final Context context;
    private final HashMap<ComponentData, LinkedList<MetricData>> metrics = new HashMap<ComponentData, LinkedList<MetricData>>(); 

    private boolean delivered = false;

    /**
     * Constructs a {@code Request} with a given {@link Context}.
     * <p> See also {@link Context#createRequest()}
     * @param context the {@link Context} for the {@code Request}
     */
    public Request(Context context) {
        this.context = context;
    }

    /**
     * Add metric to the {@code Request} for a given component.
     * The {@link Number} value is converted to a {@code float}.
     * The count is assumed to be 1, while minValue and maxValue are set to value.
     * Sum of squares is calculated as the value squared.
     * @param component the {@code ComponentData} the metric should be added to
     * @param name the name of the metric
     * @param value the Number value for the metric
     * @return MetricData the newly added metric
     */
    public MetricData addMetric(ComponentData component, String name, Number value) {
        MetricData metricData = null;
        if (value != null) {
           metricData = addMetric(component, new MetricData(name, value));
        }
        return metricData;
    }

    /**
     * Add metric to the {@code Request} for a given component.
     * All {@link Number} values are converted to {@code floats}.
     * @param component the {@code ComponentData} the metric should be added to
     * @param name the name of the metric
     * @param count the number of things being measured
     * @param value the Number value for the metric
     * @param minValue the minimum Number value for the metric
     * @param maxValue the maximum Number value for the metric
     * @param sumOfSquares the sum of squared values for the metric
     * @return MetricData the newly added metric
     */
    public MetricData addMetric(ComponentData component, String name, int count, Number value, Number minValue, Number maxValue, Number sumOfSquares) {
        MetricData metricData = null;
        if (value != null && minValue != null && maxValue != null && sumOfSquares != null) {
            metricData = addMetric(component, new MetricData(name, count, value, minValue, maxValue, sumOfSquares));
        }
        return metricData;
    }

    /**
     * Deliver the {@code Request} to the New Relic metrics API.
     */
    public void deliver() {
        // do not send an empty request
        if (metrics.isEmpty()) {
            logger.debug("No metrics were reported for this poll cycle");
        } else {
            HttpURLConnection connection = null;
            
            try {
                Map<String, Object> data = serialize();
                String json = JSONObject.toJSONString(data);
                logger.debug("Sending JSON: ", json);
                
                connection = context.createUrlConnectionForOutput();
                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                
                try {
                    out.write(json);
                } finally {
                    out.close();
                }
                
                // process and log response from the collector
                processResponse(connection);
            }
            catch (Exception ex) {
                logger.error(ex, "An error occurred communicating with the New Relic service");
    
                if (connection != null) {
                    try {
                        logger.info("Response: ", connection.getResponseCode(), " : ", connection.getResponseMessage());
                     } catch (IOException e) {
                        logger.debug(ex, ex.getMessage());
                    }
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }

    /**
     * Is the request delivered
     * @return boolean
     */
    /* package */ boolean isDelivered() {
        return delivered;
    }
    
    /**
     * Process response and log response as appropriate.
     * @param connection
     * @throws IOException
     */
    private void processResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();

        // do not log 503 responses
        if (isCollectorUnavailable(responseCode)) {
            logger.debug("Collector temporarily unavailable...continuing");
        }
        else {
            // read server response
            String responseBody = getServerResponse(responseCode, connection);
        
            if (isResponseEmpty(responseBody)) {
                logger.info("Failed server response: no response, with response code: ", responseCode);
            }
            else if (isRemotelyDisabled(responseCode, responseBody)) {
                // Remote disabling by New Relic -- exit
                logger.fatal("Agent has been disabled remotely by New Relic");
                System.err.println("SEVERE: Agent has been disabled remotely by New Relic");
                System.exit(EXIT_CODE);
            }
            else if (isResponseOk(responseCode, responseBody)) {
                logger.debug("Server response: ", responseCode, ", ", responseBody);
                delivered = true;
                Date deliveredAt = new Date();
                context.setAggregationStartedAt(deliveredAt);
                // update last successful timestamps
                updateComponentTimestamps(deliveredAt);
            }
            else {
                // all other response codes will fail
                logger.error("Failed server response: ", responseCode, ", ", responseBody);
            }
        }
    }
    
    /**
     * Checks if the Collector is currently unavailable.
     * A 503 response code (HTTP_UNAVAILABLE) from the Collector is an unavailable response.
     * @param responseCode
     * @return boolean
     */
    private boolean isCollectorUnavailable(int responseCode) {
        return responseCode == HttpURLConnection.HTTP_UNAVAILABLE;
    }
    
    /**
     * Checks if the Collector response is empty
     * @param responseBody
     * @return boolean
     */
    private boolean isResponseEmpty(String responseBody) {
        return responseBody == null || EMPTY_STRING.equals(responseBody);
    }
    
    /**
     * Checks if the agent has been remotely disabled by the Collector.
     * A 403 response code (HTTP_FORBIDDEN) with DISABLE_NEW_RELIC as the response body
     * indicate that the agent should be remotely disabled.
     * @param responseCode
     * @param responseBody
     * @return boolean
     */
    private boolean isRemotelyDisabled(int responseCode, String responseBody) {
        return responseCode == HttpURLConnection.HTTP_FORBIDDEN && DISABLE_NEW_RELIC.equals(responseBody);
    }
    
    /**
     * Checks if the Collector response is Ok.
     * A 200 response code (HTTP_OK) with status of "ok" in the response body is an Ok response.
     * @param responseCode
     * @param responseBody
     * @return boolean
     */
    private boolean isResponseOk(int responseCode, String responseBody) {
        // parse json response for status message
        String statusMessage = getStatusMessage(responseBody);
        return responseCode == HttpURLConnection.HTTP_OK && OK_STATUS.equals(statusMessage);
    }
    
    /**
     * Get server response from provided input stream
     * @param responseCode
     * @param connection
     * @return String the server response
     * @throws IOException
     */
    private String getServerResponse(int responseCode, HttpURLConnection connection) throws IOException {
    
        InputStream input = getResponseStream(responseCode, connection);
    
        StringBuilder builder = new StringBuilder();
        if (input != null) {
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    builder.append(inputLine);
                }
            } finally {
                in.close();
            }
        }
        return builder.toString();
    }
    
    /**
     * Get an InputStream from the server response.
     * Valid responses have response codes less than 400 (bad request).
     * Valid responses are read from getInputStream(), while error responses must be read from getErrorStream()
     * @param responseCode
     * @param connection
     * @return InputStream
     * @throws IOException
     */
    private InputStream getResponseStream(int responseCode, HttpURLConnection connection) throws IOException {
        return (responseCode < HttpURLConnection.HTTP_BAD_REQUEST) ? connection.getInputStream() : connection.getErrorStream();
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
        if (json != null) {
            return (String) json.get(STATUS);
        } else {
            return null;  
        }
    }
    
    /**
     * Update component timestamps for last successful reported.
     */
    private void updateComponentTimestamps(Date deliveredAt) {
        for (ComponentData component : metrics.keySet()) {
            component.setLastSuccessfulReportedAt(deliveredAt);
        }
    }
    
    /* package */ Map<String, Object> serialize() {
        return context.serialize(this);
    }

    /* package */ List<MetricData> getMetrics(ComponentData component) {
        if( ! metrics.containsKey(component)) {
            metrics.put(component, new LinkedList<MetricData>());
        }
        return metrics.get(component);
    }

    private MetricData addMetric(ComponentData component, MetricData metric) {
        logger.debug(component, " : ", metric);
        List<MetricData> metrics = getMetrics(component);
        if (metrics.contains(metric)) {
            aggregate(metric, metrics);
        } else {
            metrics.add(metric);
        }
        return metric;
    }

    private void aggregate(MetricData metric, List<MetricData> metrics) {
        for (MetricData existingMetric : metrics) {
            if (existingMetric.name.equals(metric.name)) {
                existingMetric.aggregrateWith(metric);
                break;
            }
        }
    }
}
