package com.newrelic.metrics.publish.internal;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.binding.ComponentData;
import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.binding.Request;

/**
 * Associates an Agent with a new Request for collecting data from the agent.
 */
public class DataCollector {
	
	/* package */ static final String METRIC_PREFIX = "Component/";
	/* package */ static final String DEFAULT_HOST = "host";
	/* package */ static final int DEFAULT_PID = 0;
	
	private ComponentData componentData;
	private Request request;
	
	//  Ruby version had count but we can get it off the Request
	//	private int count;
	private Context context;
	private final Agent agent;
	
	public DataCollector(Agent agent) {
		this.agent = agent;
		
		//The agentData and componentData parts of the Request remain for the duration of this instance
		context = new Context();
		context.agentData.host = DEFAULT_HOST;
		context.agentData.version = agent.getVersion();
		context.agentData.pid = DEFAULT_PID;
		componentData = context.createComponent();
		componentData.guid = agent.getGUID();
		componentData.name = agent.getComponentHumanLabel();
		//TODO duration should be computed from time since last poll
//		componentData.duration = pollInterval;
	}
	
	public Agent getAgent() {
		return agent;
	}
	
	public Context getContext() {
		return context;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public void addData(String metricName, String units, int data) {
		String metricFullName = METRIC_PREFIX + metricName + "[" + units + "]";
		request.addMetric(componentData, metricFullName, data);
	}
	
	public void addData(String metricName, String units, float data) {
		String metricFullName = METRIC_PREFIX + metricName + "[" + units + "]";
		request.addMetric(componentData, metricFullName, data);
	}
}
