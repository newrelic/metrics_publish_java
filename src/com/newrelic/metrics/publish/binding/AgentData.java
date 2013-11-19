package com.newrelic.metrics.publish.binding;

import java.util.HashMap;

/**
 * Provisional API which is subject to change.
 * Represents an agent for a given {@link Request}.
 */
public class AgentData {
    private static final String HOST = "host";
    private static final String VERSION = "version";
    private static final String PID = "pid";
    
    public String host = HOST;
    public String version;
    public int pid = 0;

    /* package */ AgentData() {
        super();
    }

    /* package */ HashMap<String, Object> serialize() {
        HashMap<String, Object> output = new HashMap<String, Object>();
        output.put(HOST, host);
        output.put(VERSION, version);
        output.put(PID, pid);
        return output;
    }

}
