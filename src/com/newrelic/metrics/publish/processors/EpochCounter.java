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
     * Process a metric value over a time interval. 
     * Calling process for a metric value at an interval less than 1 second is not supported. Null is returned for sub-second processing.
     */
    @Override
    public Number process(Number val) {
        Date currentTime = new Date();
        Number ret = null;

        if (lastValue != null && lastTime != null && currentTime.after(lastTime)) {
            long timeDiffInSeconds = (currentTime.getTime() - lastTime.getTime()) / 1000;
            if (timeDiffInSeconds > 0) {
                ret = (val.floatValue() - lastValue.floatValue()) / timeDiffInSeconds;
                if (ret.floatValue() < 0) {
                    ret = null;
                }
            }
        }

        lastValue = val;
        lastTime = currentTime;
        return ret;
    }
}
