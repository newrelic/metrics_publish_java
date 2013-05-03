package com.newrelic.metrics.publish.processors;

public interface Processor {
	public Number process(Number val);
}
