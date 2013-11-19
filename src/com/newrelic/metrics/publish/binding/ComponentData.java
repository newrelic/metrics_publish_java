package com.newrelic.metrics.publish.binding;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Provisional API which is subject to change.
 * Represents a component that reported metrics will be associated with for a {@link Request}.
 */
public class ComponentData {

    private static final String NAME = "name";
    private static final String GUID = "guid";
    private static final String DURATION = "duration";
    private static final String METRICS = "metrics";
    
    public String name;
    public String guid;
    private Date lastSuccessfulReportedAt;

    /* package */ ComponentData() {
        super();
    }

    /* package */ HashMap<String,Object> serialize(Request request) {
        HashMap<String, Object> output = new HashMap<String, Object>();

        List<MetricData> metrics = request.getMetrics(this);

        if(!metrics.isEmpty()) {
            output.put(NAME, name);
            output.put(GUID, guid);

            output.put(DURATION, calculateDuration());

            HashMap<String, Object> metricsOutput = new HashMap<String, Object>();
            output.put(METRICS, metricsOutput);

            for (MetricData metric : metrics) {
                metric.serialize(metricsOutput);
            }
        }

        return output;
    }

    /**
     * Set date timestamp for the last successful report
     * @param lastSuccessfulReportedAt the date of the last successful report
     */
    /* package */ void setLastSuccessfulReportedAt(Date lastSuccessfulReportedAt) {
        this.lastSuccessfulReportedAt = lastSuccessfulReportedAt;
    }

    /**
     * Calculate duration from last successful reported timestamp.
     * If last timestamp isn't set, return 60 as default duration.
     * Otherwise, return the time difference between now and the last successful reported timestamp.
     * @return the duration
     */
    private int calculateDuration() {
        if (lastSuccessfulReportedAt == null) {
            return 60; // default duration
        }
        long now = new Date().getTime();
        // round up duration to whole second
        long duration = (long) Math.ceil((now - lastSuccessfulReportedAt.getTime()) / 1000.0);
        return (int) duration;
    }

    public String toString() {
        return new StringBuilder().append("ComponentData").append("(").append(name).append(":").append(guid).append(")").toString();
    }
}
