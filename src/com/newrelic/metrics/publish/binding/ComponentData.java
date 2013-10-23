package com.newrelic.metrics.publish.binding;

import java.util.Date;
import java.util.HashMap;

/**
 * Provisional API which is subject to change.
 * Represents a component that reported metrics will be associated with for a {@link Request}.
 */
public class ComponentData {

    public String name;
    public String guid;
    private Date lastSuccessfulReportedAt;

    /* package */ ComponentData() {
        super();
    }

    /* package */ HashMap<String,Object> serialize(Request request) {
        HashMap<String, Object> output = new HashMap<String, Object>();
        output.put("name", name);
        output.put("guid", guid);

        output.put("duration", calculateDuration());

        HashMap<String, Object> metricsOutput = new HashMap<String, Object>();
        output.put("metrics", metricsOutput);

        for (MetricData metric : request.getMetrics(this)) {
            metric.serialize(metricsOutput);
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
        return "ComponentData(" + name + ":" + guid + ")";
    }
}
