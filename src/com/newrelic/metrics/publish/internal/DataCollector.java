package com.newrelic.metrics.publish.internal;

import com.newrelic.metrics.publish.binding.ComponentData;
import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.binding.Request;

/**
 * An internal class that collects data for an {@link Agent}
 * and associates it with a new {@link Request}.
 */
public class DataCollector {

    /* package */ static final String METRIC_PREFIX = "Component/";
    private static final String LEFT_BRACKET = "[";
    private static final String RIGHT_BRACKET = "]";
    private static final int METRIC_STRING_BASE_LENGTH = 12; // METRIC_PREFIX length plus BRACKET lengths

    private ComponentData componentData;
    private Request request;

    //  Ruby version had count but we can get it off the Request
    //  private int count;
    private Context context;

    /**
     * Constructs a {@code DataCollector}.
     */
    public DataCollector() {}

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
        this.context = context;
    }
    
    public void createComponent(String guid, String componentName) {
        if (componentData == null) {
            // The agentData and componentData parts of the Request remain for the duration of this instance
            componentData = context.createComponent();
            componentData.guid = guid;
            componentData.name = componentName;
        }
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

    /* package */ String getMetricFullName(String metricName, String units) {
        // allocating exact size to reduce memory in array resizing in StringBuilder
        return new StringBuilder(METRIC_STRING_BASE_LENGTH + metricName.length() + units.length())
            .append(METRIC_PREFIX)
            .append(metricName)
            .append(LEFT_BRACKET)
            .append(units)
            .append(RIGHT_BRACKET)
            .toString();
    }
}
