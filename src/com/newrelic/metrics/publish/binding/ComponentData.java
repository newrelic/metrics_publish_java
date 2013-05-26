package com.newrelic.metrics.publish.binding;

import java.util.HashMap;

public class ComponentData {
	
	private final String name;
	private final String guid;
	
	/* package */ ComponentData(String name, String guid) {
		super();
		this.name = name;
		this.guid = guid;
	}
	
	public String getName() {
		return name;
	}
	
	public String getGUID() {
		return guid;
	}

	/* package */ HashMap<String,Object> serialize(Request request) {
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("name", name);
		output.put("guid", guid);	
		output.put("duration", request.getDuration());	
		
		HashMap<String, Object> metricsOutput = new HashMap<String, Object>();
		output.put("metrics", metricsOutput);
		
		for (MetricData metric : request.getMetrics(this)) {
			metric.serialize(metricsOutput);
		}
		
		return output;
	}	
	
	public String toString() {
		return "ComponentData(" + name + ":" + guid + ")";
	}
}
