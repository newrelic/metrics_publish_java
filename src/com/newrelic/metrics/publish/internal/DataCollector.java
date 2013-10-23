package com.newrelic.metrics.publish.internal;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.binding.ComponentData;
import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.binding.Request;

/**
 * An internal class that collects data for an {@link Agent}
 * and associates it with a new {@link Request}.
 */
public class DataCollector {

    /* package */ static final String METRIC_PREFIX = "Component/";

    private ComponentData componentData;
    private Request request;

    //  Ruby version had count but we can get it off the Request
    //  private int count;
    private Context context;
    private final Agent agent;

    /**
     * Constructs a {@code DataCollector} with the provided {@link Agent}.
     * @param agent the {@link Agent} to construct with
     */
    public DataCollector(Agent agent) {
        this.agent = agent;
    }

    /**
     * Get the {@link Agent}
     * @return agent
     */
    public Agent getAgent() {
        return agent;
    }

    /**
     * Get the {@link Context}
     * @return context
     */
    public Context getContext() {
        return context;
    }

    /**
     * Set the {@link Context}
     * @param context
     */
    public void setContext(Context context) {
        //The agentData and componentData parts of the Request remain for the duration of this instance
        componentData = context.createComponent();
        componentData.guid = agent.getGUID();
        componentData.name = agent.getComponentHumanLabel();
    }

    /**
     * Set the {@link Request}
     * @param request
     */
    public void setRequest(Request request) {
        this.request = request;
    }

    /**
     * Add metric data to the {@code DataCollector}.
     * The {@link Number} value is converted to a {@code float}.
     * The count is assumed to be 1, while minValue and maxValue are set to value.
     * Sum of squares is calculated as the value squared.
     * @param metricName the name of the metric to add
     * @param units the units of the metric
     * @param data the Number data value of the metric
     */
    public void addData(String metricName, String units, Number value) {
        request.addMetric(componentData, getMetricFullName(metricName, units), value);
    }

    /**
     * Add metric data to the {@code DataCollector}.
     * All {@link Number} data values are converted to {@code floats}.
     * @param metricName the name of the metric to add
     * @param units the units of the metric
     * @param count the number of things being measured
     * @param value the Number value of the metric
     * @param minValue the minimum Number value of the metric
     * @param maxValue the maximum Number value of the metric
     * @param sumOfSquares the sum of squared values of the metric
     */
    public void addData(String metricName, String units, int count, Number value, Number minValue, Number maxValue, Number sumOfSquares) {
        request.addMetric(componentData, getMetricFullName(metricName, units), count, value, minValue, maxValue, sumOfSquares);
    }

    private String getMetricFullName(String metricName, String units) {
        return METRIC_PREFIX + metricName + "[" + units + "]";
    }
}
