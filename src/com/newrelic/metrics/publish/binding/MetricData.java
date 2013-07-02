package com.newrelic.metrics.publish.binding;

import java.util.HashMap;

/**
 * Represents a metric for reporting to the New Relic metrics API.
 */
public class MetricData {
	/* package */ String name;
	/* package */ Number value;  //must be int or float
	
	/* package */ MetricData(String name, Number value) {
		super();
		init(name, value);
	}
	
	/* package */ void init(String name, Number value) {
		this.name = name;
		this.value = value;
	}
	
	/* package */ void serialize(HashMap<String, Object> data) {
		Context.getLogger().fine("Metric: " + name + " value: " + value);
		data.put(name, value);	
	}
	
	public String toString() {
		return "Metric(" + name + " : " + value + ")";
	}
}
