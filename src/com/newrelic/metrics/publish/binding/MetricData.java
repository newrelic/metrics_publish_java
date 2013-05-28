package com.newrelic.metrics.publish.binding;

import java.util.HashMap;

public class MetricData {
	/* package */ String name;
	/* package */ Number value;  //must be int or float
	
	/* package */ MetricData(String name, Number value) {
		super();
		this.name = name;
		this.value = value;
	}
	
	/* package */ void serialize(HashMap<String, Object> data) {
		Context.getLogger().finest("Metric: " + name + " value: " + value);
		data.put(name, value);	
	}
	
	public String toString() {
		return "Metric(" + name + " : " + value + ")";
	}
}
