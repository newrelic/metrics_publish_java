package com.newrelic.metrics.publish.processors;

import java.util.Date;

/**
 * A {@link Processor} for metric values over a time interval. 
 */
public class EpochCounter implements Processor {
	private Number lastValue;
	private Date lastTime;

	/**
     * Constructs an {@code EpochCounter}
     */
	public EpochCounter() {
		super();
	}

	/**
     * Process a metric value over a time interval
     */
	@Override
	public Number process(Number val) {
		Date currentTime = new Date();
		Number ret = null;
		
		if(lastValue != null && lastTime != null && currentTime.after(lastTime)) {
			long timeDiff = (currentTime.getTime() - lastTime.getTime()) / 1000;
			ret = (val.floatValue()-lastValue.floatValue())/timeDiff;
			if(ret.floatValue() < 0) {
				ret = null;
			}
		}
		
		lastValue = val;
		lastTime = currentTime;
		return ret;
	}
}
