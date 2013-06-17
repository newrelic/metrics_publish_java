package com.newrelic.metrics.publish;

import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.internal.DataCollector;

/**
 * An extensible class for gathering metrics from a system.
 * An {@code Agent} can be constructed directly with a GUID (Globally Unique Identifier)
 * and a version or configured with an {@link AgentFactory} through a JSON properties file.
 * The GUID should be similar to the reverse of a DNS name; for example: {@code com.some_company.some_plugin_name}
 * <p> All subclasses must override {@link #pollCycle()} and {@link #getComponentHumanLabel()}. 
 * Additional hooks are provided for overriding: {@link #setupMetrics()} and {@link #prepareToRun()}. These hooks must call {@code super}.
 */
public abstract class Agent {

	private final String GUID;
	private final String version;
	//TODO in Ruby, this is called a "agent_human_label" but they're really labels for extensions and components

	private DataCollector collector;

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
     * @return String the component human label
     */
	public abstract String getComponentHumanLabel();
	
	/**
     * A hook called when the {@code Agent} is setup.
     * Subclasses may override but must call {@code super}.
     */
	public void setupMetrics() {
		Context.getLogger().fine("setupMetrics");
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
		//This needs to be done after being configured to ensure the binding model created by the DataCollector
		//has the most recent values from this
		collector = new DataCollector(this);
	}
	
	/**
	 * Report a metric with a name, unit(s) and value. 
	 * Value may be {@code null} in which case the reporting is skipped.
	 * @param metricName the name of the metric
	 * @param units the units to report
	 * @param value the Number value to report
	 */
	public void reportMetric(String metricName, String units, Number value) {
	    if (value != null) {
	        Context.getLogger().fine("Reporting metric: " + metricName);
	        collector.addData(metricName, units, value);
	    }
	}

	/* package */ DataCollector getCollector() {
		return collector;
	}
}
