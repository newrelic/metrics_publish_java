package com.newrelic.metrics.publish.internal;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.binding.ComponentData;
import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.binding.Request;

/**
 * An internal class that collects data for an {@link Agent}
 * and associates it with a new {@link Request}.
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
	
	/**
     * Constructs a {@code DataCollector} with the provided {@link Agent}.
     * @param agent the {@link Agent} to construct with
     */
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
	
	/**
     * Get the {@link Agent} 
     * @return agent
     */
	public Agent getAgent() {
		return agent;
	}
	
	/**
     * Get the {@link Context}
     * @return context
     */
	public Context getContext() {
		return context;
	}

	/**
     * Set the {@link Request}
     * @param request
     */
	public void setRequest(Request request) {
		this.request = request;
	}

	/**
	 * Add data to the {@code DataCollector}
	 * @param metricName the name of the metric to add
	 * @param units the units of the metric
	 * @param data the Number data value of the metric
	 */
	public void addData(String metricName, String units, Number data) {
		String metricFullName = METRIC_PREFIX + metricName + "[" + units + "]";
		request.addMetric(componentData, metricFullName, data);
	}

}
