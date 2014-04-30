package com.newrelic.metrics.publish;

import com.newrelic.metrics.publish.internal.DataCollector;
import com.newrelic.metrics.publish.util.Logger;

/**
 * An extensible class for gathering metrics from a system.
 * An {@code Agent} can be constructed directly with a GUID (Globally Unique Identifier)
 * and a version or configured with an {@link AgentFactory} through a JSON properties file.
 * The GUID should be similar to the reverse of a DNS name; for example: {@code com.some_company.some_plugin_name}
 * <p> All subclasses must override {@link #pollCycle()} and {@link #getComponentHumanLabel()}.
 * Additional hooks are provided for overriding: {@link #setupMetrics()} and {@link #prepareToRun()}. These hooks must call {@code super}.
 */
public abstract class Agent {

    private static final Logger logger = Logger.getLogger(Agent.class);

    private static final String REPORTING_METRIC_MSG = "Reporting metric: ";

    private final String GUID;
    private final String version;
    //TODO in Ruby, this is called a "agent_human_label" but they're really labels for extensions and components

    private final DataCollector collector;

    /**
     * Constructs an {@code Agent} with provided GUID (Globally Unique Identifier) and version.
     * The GUID should be similar to the reverse of a DNS name; for example: {@code com.some_company.some_plugin_name}
     * The version should be incremented prior to publishing a modified agent.
     * @param GUID
     * @param version
     */
    public Agent(String GUID, String version) {
        super();
        this.GUID = GUID;
        this.version = version;
        this.collector = new DataCollector();
    }

    /**
     * The {@code Agent} will gather and report metrics from this method during every poll cycle.
     * It is called by the {@link Runner} at a set interval and is run in a loop that never returns.
     * <p> This method must be overridden by subclasses of {@code Agent}.
     */
    public abstract void pollCycle();

    /**
     * A human readable label for the component that this {@code Agent} is reporting metrics on.
     * <p> This method must be overridden by subclasses of {@code Agent}.
     * <p> This replaces the deprecated 'getComponentHumanLabel()' method.
     * @return A name representing an instance of an Agent
     */
    public abstract String getAgentName();

    /**
     * A hook called when the {@code Agent} is setup.
     * Subclasses may override but must call {@code super}.
     */
    public void setupMetrics() {
        logger.debug("Setting up metrics");
    }

    /**
     * Get the GUID (Globally Unique Identifier)
     * @return String the GUID
     */
    public String getGUID() {
        return GUID;
    }

    /**
     * Get the version
     * @return String the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * A hook called when the {@code Agent} is setup.
     * Subclasses may override but must call {@code super}.
     */
    public void prepareToRun() {
        logger.debug("Preparing to run");
    }

    /**
     * Report a metric with a name, unit(s) and value.
     * The {@link Number} value is converted to a {@code float}.
     * The count is assumed to be 1, while minValue and maxValue are set to value.
     * Sum of squares is calculated as the value squared.
     * If the value is {@code null}, the reporting is skipped.
     * @param metricName the name of the metric
     * @param units the units to report
     * @param value the Number value to report
     */
    public void reportMetric(String metricName, String units, Number value) {
        if (value != null) {
            logger.debug(REPORTING_METRIC_MSG, metricName);
            collector.addData(metricName, units, value);
        }
    }

    /**
     * Report a metric with a name, unit(s), count, value, minValue, maxValue and sumOfSquares.
     * All {@link Number} values are converted to {@code floats}.
     * If any of the values are {@code null}, the reporting is skipped.
     * @param metricName the name of the metric
     * @param units the units to report
     * @param count the number of things being measured
     * @param value the Number value to report
     * @param minValue the minimum Number value to report
     * @param maxValue the maximum Number value to report
     * @param sumOfSquares the sum of squared values to report
     */
    public void reportMetric(String metricName, String units, int count, Number value, Number minValue, Number maxValue, Number sumOfSquares) {
        if (value != null && minValue != null && maxValue != null && sumOfSquares != null) {
            logger.debug(REPORTING_METRIC_MSG, metricName);
            collector.addData(metricName, units, count, value, minValue, maxValue, sumOfSquares);
        }
    }

    /* package */ DataCollector getCollector() {
        return collector;
    }
}
