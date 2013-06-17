package com.newrelic.metrics.publish.binding;

import java.util.HashMap;

/**
 * Represents a metric for reporting to the New Relic metrics API.
 */
public class MetricData {
	/* package */ String name;
	/* package */ Number value;  //must be int or float
	/* package */ ComponentData componentData;
	
	/* package */ MetricData(ComponentData componentData, String name, Number value) {
		super();
		init(componentData, name, value);
	}
	
	/* package */ void init(ComponentData componentData, String name, Number value) {
		this.name = name;
		this.value = value;
		this.componentData = componentData;
	}
	
	/* package */ void serialize(HashMap<String, Object> data) {
		Context.getLogger().fine("Metric: " + name + " value: " + value);
		data.put(name, value);	
	}
	
	public String toString() {
		return "Metric(" + name + " : " + value + ")";
	}
}
