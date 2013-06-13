package com.newrelic.metrics.publish;

import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.internal.DataCollector;

public abstract class Agent {

	private final String GUID;
	private final String version;
	//TODO in Ruby, this is called a "agent_human_label" but they're really labels for extensions and components

	private DataCollector collector;

	public Agent(String GUID, String version) {
		super();
		this.GUID = GUID;
		this.version = version;
	}
	
	//Must provide implementations
	public abstract void pollCycle();	
	public abstract String getComponentHumanLabel();
	
	//You can but don't need to override this
	public void setupMetrics() {
		Context.getLogger().fine("setupMetrics");
	}
	
	public String getGUID() {
		return GUID;
	}
	public String getVersion() {
		return version;
	}
	
	/**
	 * Subclasses may override but must call super.
	 */
	public void prepareToRun() {
		//This needs to be done after being configured to ensure the binding model created by the DataCollector
		//has the most recent values from this
		collector = new DataCollector(this);
	}
	
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
