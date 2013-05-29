package com.newrelic.metrics.publish;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.binding.Request;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.newrelic.metrics.publish.configuration.SDKConfiguration;

/**
 * The main entry point for executing the SDK.
 * Register agent instances that get run in a loop that never returns.
 * @author kevin-mcguire
 *
 */
public class Runner {

	private List<Agent> agents;
	private final SDKConfiguration config;
	private int pollInterval = 60;
	private HashSet<AgentFactory> factories = new HashSet<AgentFactory>();
	
	public Runner() {
		super();
		agents = new LinkedList<Agent>();

		Logger logger = Context.getLogger();
        boolean loggerConfigured = logger.getHandlers().length > 0; 
        if (!loggerConfigured) {
            logger.warning("The logger is not configured");
        }   
      
        try {
            config = new SDKConfiguration();
        } catch (ConfigurationException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
	}
	
	public void add(AgentFactory factory) {
		factories.add(factory);
	}

	public void register(Agent agent) {
		agents.add(agent);
	}

	public SDKConfiguration getConfiguration() {
		return config;
	}

    public void setupAndRun() throws ConfigurationException {        
    	setupAgents();
        pollInterval = config.getPollInterval();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();  
        executor.scheduleAtFixedRate(new PollAgentsRunnable(), 0, pollInterval, TimeUnit.SECONDS);  //schedule ourself as the runnable command
        
        Context.getLogger().info("New Relic monitor started");
        
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

	private void createAgents() throws ConfigurationException {
        for (Iterator<AgentFactory> iterator = factories.iterator(); iterator.hasNext();) {
			AgentFactory factory = (AgentFactory) iterator.next();
			factory.createConfiguredAgents(this);
		}
	}
	
    private void setupAgents() throws ConfigurationException {
    	Context.getLogger().fine("Setting up agents to be run");
        createAgents();
        if(config.internalGetServiceURI() != null) {
        	Context.getLogger().info("Using host: " + config.internalGetServiceURI());
        }

        for (Iterator<Agent> iterator = agents.iterator(); iterator.hasNext();) {
			Agent agent = iterator.next();
			agent.prepareToRun();
	        agent.setupMetrics();
	        //TODO this is a really awkward place to set the license key on the request
	        agent.getCollector().getContext().licenseKey = config.getLicenseKey();
	        if(config.internalGetServiceURI() != null) {
	        	agent.getCollector().getContext().internalSetServiceURI(config.internalGetServiceURI());
	        }
		}
    }
    
    /**
     * Inner runnable class for polling agents from ScheduledExecutor
     * @author jstenhouse
     */
    private class PollAgentsRunnable implements Runnable {
    	
    	/**
         * Collect and report metric data.
         */
        @Override
        public void run() {
        	Context.getLogger().fine("Harvest and report data");
            for (Iterator<Agent> iterator = agents.iterator(); iterator.hasNext();) {
    			Agent agent = iterator.next();
            	Request request = new Request(agent.getCollector().getContext(), pollInterval);
            	//todo set poll interval
            	agent.getCollector().setRequest(request);
    	        agent.pollCycle();
    	        request.send();			
            	agent.getCollector().setRequest(null); //make sure we're not reusing the request
    		}
        }
    }
}
