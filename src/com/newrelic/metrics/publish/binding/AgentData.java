package com.newrelic.metrics.publish.binding;

import java.util.HashMap;

public class AgentData {
	public String host;
	public String version;
	public int pid;
	
	protected AgentData() {
		super();
	}
	
	protected HashMap<String, Object> serialize() {
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("host", host);
		output.put("version", version);
		output.put("pid", pid);
		return output;
	}
	
}
