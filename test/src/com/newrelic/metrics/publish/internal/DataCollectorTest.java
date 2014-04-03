package com.newrelic.metrics.publish.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DataCollectorTest {

    @Test
    public void testGetMetricFullName() {
        DataCollector collector = new DataCollector();
        
        assertEquals("Component/Queries[queries/second]", collector.getMetricFullName("Queries", "queries/second"));
        assertEquals("Component/DB/Queries[queries/second]", collector.getMetricFullName("DB/Queries", "queries/second"));
        assertEquals("Component/Network/Bytes[count]", collector.getMetricFullName("Network/Bytes", "count"));
    }
}
