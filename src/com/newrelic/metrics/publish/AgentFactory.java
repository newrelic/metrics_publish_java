package com.newrelic.metrics.publish;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

/**
 * AgentFactory has two roles:
 * 1) Create new instances of an agent
 * 2) Map read in configuration tables to configured state of new agents
 */
public abstract class AgentFactory {
	private final String agentConfigurationFileName;
	private boolean configRequired = true;
	
//	TODO system property for config path
	private static final String CONFIG_PATH = "config";
	
	public AgentFactory(String agentConfigFileName) {
		super();
		this.agentConfigurationFileName = agentConfigFileName;
		this.configRequired = true;
	}

	public AgentFactory() {
		super();
		this.agentConfigurationFileName = null;
		this.configRequired = false;
	}
	
	/**
	 * Return a new instance of the appropriate Agent subclass, configured with information
	 * extracted from the @properties, a Map of configuration keys and values.
	 * 
	 * The keys and values are the result of processing the file referred to by
	 * getAgentConfigurationFileName().
	 * The specific keys and legal values are specific to the domain of the agent.
	 * Since the values come in as Object, casting and conversion may be required.
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
	
	/* protected */ void createConfiguredAgents(Runner runner) throws ConfigurationException {
        if(configRequired) {
            JSONArray json = readJSONFile(getAgentConfigurationFileName());
        
	        for (int i = 0; i < json.size(); i++) {
	        	JSONObject obj = (JSONObject) json.get(i);
	        	@SuppressWarnings("unchecked")
	        	Map<String, Object> map = obj;
	        	createAndRegister(runner, map);
			}
        } else {
        	createAndRegister(runner, null);
        }
	}

	public JSONArray readJSONFile(String filename) throws ConfigurationException {
		Object parseResult = null;
		
		File file = getConfigurationFile(filename);

		try {
	   	    FileReader reader = new FileReader(file);        
		    JSONParser parser = new JSONParser();
		    
		    try {
		    	parseResult = parser.parse(reader);
			} catch (ParseException e) {
				throw logAndThrow(Context.getLogger(), "Error parsing config file " + file.getAbsolutePath());
			}
		} catch(IOException ioEx) {
			throw logAndThrow(Context.getLogger(), "Error reading config file " + file.getAbsolutePath());
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
        	throw logAndThrow(Context.getLogger(), "Cannot find config file " + path);
        }
        return file;
	}
    
    private ConfigurationException logAndThrow(Logger logger,String message) {
        logger.severe(message);
        return new ConfigurationException(message);
    }    
}