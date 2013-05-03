package com.newrelic.platform.metrics.publish.processors;

public interface Processor {
	public Number process(Number val);
}
