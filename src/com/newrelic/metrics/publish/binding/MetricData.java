package com.newrelic.metrics.publish.binding;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Provisional API which is subject to change.
 * Represents a metric for reporting to the New Relic metrics API.
 * All metric values are stored as {@code floats}.
 */
public class MetricData {
    /* package */ String name;
    /* package */ int count;
    /* package */ float value;
    /* package */ float minValue;
    /* package */ float maxValue;
    /* package */ float sumOfSquares;

    /* package */ MetricData(String name, Number value) {
        this(name, 1, value.floatValue(), value.floatValue(), value.floatValue(), value.floatValue() * value.floatValue());
    }

    /* package */ MetricData(String name, int count, Number value, Number minValue, Number maxValue, Number sumOfSquares) {
        this.name = name;
        this.count = count;
        this.value = value.floatValue();
        this.minValue = minValue.floatValue();
        this.maxValue = maxValue.floatValue();
        this.sumOfSquares = sumOfSquares.floatValue();
        convertValues();
    }

    /* package */ void serialize(HashMap<String, Object> data) {
        data.put(name, Arrays.<Number>asList(value, count, minValue, maxValue, sumOfSquares));
    }

    /* package */ void aggregrateWith(MetricData other) {
        count += other.count;
        value += other.value;
        minValue = Math.min(minValue, other.minValue);
        maxValue = Math.max(maxValue, other.maxValue);
        sumOfSquares += other.sumOfSquares;
        convertValues();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Metric: ").append(name).append(", ");
        builder.append("count: ").append(count).append(", ");
        builder.append("value: ").append(value).append(", ");
        builder.append("minValue: ").append(minValue).append(", ");
        builder.append("maxValue: ").append(maxValue).append(", ");
        builder.append("sumOfSquares: ").append(sumOfSquares);
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /**
     * Two metric data objects are equal if their names are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MetricData other = (MetricData) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
    
    private void convertValues() {
        convertInfiniteValues();
        convertNaNValues();
    }
    
    private void convertInfiniteValues() {
        if (Float.POSITIVE_INFINITY == value) { value = Float.MAX_VALUE; }
        if (Float.POSITIVE_INFINITY == maxValue) { maxValue = Float.MAX_VALUE; }
        if (Float.POSITIVE_INFINITY == minValue) { minValue = Float.MAX_VALUE; }
        if (Float.POSITIVE_INFINITY == sumOfSquares) { sumOfSquares = Float.MAX_VALUE; }
        
        if (Float.NEGATIVE_INFINITY == value) { value = Float.MIN_VALUE; }
        if (Float.NEGATIVE_INFINITY == maxValue) { maxValue = Float.MIN_VALUE; }
        if (Float.NEGATIVE_INFINITY == minValue) { minValue = Float.MIN_VALUE; }
        if (Float.NEGATIVE_INFINITY == sumOfSquares) { sumOfSquares = Float.MIN_VALUE; }
    }
    
    private void convertNaNValues() {
        if (Float.isNaN(value)) { value = 0.0f; }
        if (Float.isNaN(maxValue)) { maxValue = 0.0f; }
        if (Float.isNaN(minValue)) { minValue = 0.0f; }
        if (Float.isNaN(sumOfSquares)) { sumOfSquares = 0.0f; }
    }
}
