package com.newrelic.platform.metrics.publish.binding;

import java.util.HashMap;
// import java.util.LinkedList;

public class ComponentData {
	public String name;
	public String guid;
	protected Context context;
	
	public String toString() {
		return "ComponentData(" + name + ":" + guid + ")";
	}
	
	protected ComponentData(Context context) {
		super();
		this.context = context;
		context.add(this);
	}

	protected HashMap<String,Object> serialize(Request request) {
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
}
