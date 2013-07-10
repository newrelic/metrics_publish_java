package com.newrelic.metrics.publish.processors;

/**
 * A general purpose interface for processing metric values in {@link Agent}.
 */
public interface Processor {
    
    /**
     * Process a {@code Number} for metric reporting. 
     * @param val the Number to be processed
     * @return Number the processed Number
     */
	public Number process(Number val);
}
