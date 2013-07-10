package com.newrelic.metrics.publish;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

/**
 * A Factory for creating configured {@link Agent}s.
 * <p> {@code AgentFactory} has two roles:
 * <ol>
 * <li> Create new instances of an {@link Agent}
 * <li> Use {@code Map} of properties to configure state of new {@link Agent}s
 */
public abstract class AgentFactory {
	private final String agentConfigurationFileName;
	private boolean configRequired = true;
	
//	TODO system property for config path
	private static final String CONFIG_PATH = "config";
	
	/**
     * Constructs an {@code AgentFactory} with an agent configuration file.
     * @param agentConfigFileName the configuration file to be used for creating {@link Agent}s
     */
	public AgentFactory(String agentConfigFileName) {
		super();
		this.agentConfigurationFileName = agentConfigFileName;
		this.configRequired = true;
	}

	/**
     * Constructs an {@code AgentFactory} without an agent configuration file.
     */
	public AgentFactory() {
		super();
		this.agentConfigurationFileName = null;
		this.configRequired = false;
	}
	
	/**
     * Return a new instance of the appropriate {@link Agent} subclass, configured with information
     * extracted from the {@code properties}, a {@code Map} of configuration keys and values.
     * The keys and values are the result of processing the file referred to by
     * {@link #getAgentConfigurationFileName()}.
     * The specific keys and legal values are specific to the domain of the agent.
     * Since the values come in as {@code Object}, casting and conversion may be required.
     * @param properties the {@code Map} of properties for creating a configured {@link Agent}
     * @throws ConfigurationException if an error occurs while creating a configured {@link Agent}
    */
	public abstract Agent createConfiguredAgent(Map<String, Object> properties) throws ConfigurationException;
	
	/**
     * Returns the name of the file which holds the agent configuration information,
     * in JSON format.
     * Subclasses can override
     * @return String file name
     */
	public String getAgentConfigurationFileName() {
		return agentConfigurationFileName;
	}
	
	/* package */ void createConfiguredAgents(Runner runner) throws ConfigurationException {
        if(configRequired) {
            JSONArray json = readJSONFile(getAgentConfigurationFileName());
        
	        for (int i = 0; i < json.size(); i++) {
	        	JSONObject obj = (JSONObject) json.get(i);
	        	@SuppressWarnings("unchecked")
	        	Map<String, Object> map = obj;
	        	createAndRegister(runner, map);
			}
        } else {
        	createAndRegister(runner, new HashMap<String, Object>());
        }
	}

	/**
     * Read in JSON file from the provided filename.
     * @param filename the filename to read in
     * @return JSONArray the JSONArray that represents the JSON file
     * @throws ConfigurationException if an error occurs reading in or parsing the JSON file
     */
	public JSONArray readJSONFile(String filename) throws ConfigurationException {
		Object parseResult = null;
		
		File file = getConfigurationFile(filename);

		try {
	   	    FileReader reader = new FileReader(file);        
		    JSONParser parser = new JSONParser();
		    
		    try {
		    	parseResult = parser.parse(reader);
			} catch (ParseException e) {
				throw logAndThrow("Error parsing config file " + file.getAbsolutePath());
			}
		} catch(IOException ioEx) {
			throw logAndThrow("Error reading config file " + file.getAbsolutePath());
		}

		JSONArray json = (JSONArray) parseResult;
		return json;
	}

	private void createAndRegister(Runner runner, Map<String, Object> map) throws ConfigurationException {
    	Agent agent = createConfiguredAgent(map);
    	Context.getLogger().fine("Created agent: " + agent);
    	runner.register(agent);
	}
	
    private File getConfigurationFile(String configFileName) throws ConfigurationException {
    	String path = CONFIG_PATH + File.separatorChar + configFileName;
        File file = new File(path);
        if (!file.exists()) {
        	throw logAndThrow("Cannot find config file " + path);
        }
        return file;
	}
    
    private ConfigurationException logAndThrow(String message) {
        Context.getLogger().severe(message);
        return new ConfigurationException(message);
    }    
}