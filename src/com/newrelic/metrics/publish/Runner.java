package com.newrelic.metrics.publish;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.binding.Request;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.newrelic.metrics.publish.configuration.SDKConfiguration;

/**
 * The main entry point for executing the SDK.
 * Add an {@link AgentFactory} to create an {@link Agent}
 * or register an {@link Agent} directly. The {@code Runner} will poll {@link Agent}s
 * in a loop that never returns.
 */
public class Runner {
    
    private List<Agent> componentAgents;
    private final SDKConfiguration config;
    private int pollInterval = 60;
    private HashSet<AgentFactory> factories = new HashSet<AgentFactory>();
    private Context context;

    /**
     * Constructs a {@code Runner}
     */
    public Runner() {
        super();
        componentAgents = new LinkedList<Agent>();

        try {
            config = new SDKConfiguration();
        } catch (Exception e) {
            Context.getLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Add an {@link AgentFactory} that can create {@link Agent}s
     * through a JSON configuration file
     * @param factory the {@link AgentFactory} to be added
     */
    public void add(AgentFactory factory) {
        factories.add(factory);
    }

    /**
     * Register an {@link Agent}
     * @param agent the {@link Agent} to be registered
     */
    public void register(Agent agent) {
        componentAgents.add(agent);
    }

    /**
     * Get the {@link SDKConfiguration} for the {@code Runner}
     * @return SDKConfiguration the current {@link SDKConfiguration}
     */
    public SDKConfiguration getConfiguration() {
        return config;
    }

    /**
     * Setup the {@code Runner} and run in a loop that will never return.
     * Add an {@link AgentFactory} or register {@link Agent}s before calling.
     * @throws ConfigurationException if the {@link Runner} was not configured correctly
     */
    public void setupAndRun() throws ConfigurationException {
        setupAgents();
        pollInterval = config.getPollInterval();

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(new PollAgentsRunnable(), 0, pollInterval, TimeUnit.SECONDS);  //schedule pollAgentsRunnable as the runnable command

        System.out.println("INFO: New Relic monitor started");

        try {
            // getting the future's response will block forever unless an exception is thrown
            future.get();
        } catch (InterruptedException e) {
            System.err.println("SEVERE: An error has occurred");
            e.printStackTrace();
        } catch (CancellationException e) {
            System.err.println("SEVERE: An error has occurred");
            e.printStackTrace();
        } catch (ExecutionException e) {
            // ExecutionException will wrap any java.lang.Error from the polling thread that we should not catch there (e.g. OutOfMemoryError)
            System.err.println("SEVERE: An error has occurred");
            e.printStackTrace();
        } finally {
            // clean up and exit
            future.cancel(true);
            executor.shutdown();
            System.exit(1);
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
            Context.log(Level.INFO, "Using host: ", config.internalGetServiceURI());
        }

        context = new Context();

        for (Iterator<Agent> iterator = componentAgents.iterator(); iterator.hasNext();) {
            Agent agent = iterator.next();
            agent.prepareToRun(context);
            agent.setupMetrics();
            //TODO this is a really awkward place to set the license key on the request
            context.licenseKey = config.getLicenseKey();
            if(config.internalGetServiceURI() != null) {
                context.internalSetServiceURI(config.internalGetServiceURI());
            }
            context.internalSetSSLHostVerification(config.isSSLHostVerificationEnabled());
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
            try {
                Context.log(Level.FINE, "Harvest and report data");

                Request request = context.createRequest();

                for (Iterator<Agent> iterator = componentAgents.iterator(); iterator.hasNext();) {
                    Agent agent = iterator.next();
                    agent.getCollector().setRequest(request);
                    Context.log(Level.FINE, "Beginning poll cycle for agent: '", agent.getComponentHumanLabel(), "'");
                    agent.pollCycle();
                    Context.log(Level.FINE, "Ending poll cycle for agent: '", agent.getComponentHumanLabel(), "'");
                }

                request.deliver();
            } catch (Exception e) {
                // log exception and continue polling -- could be a transient issue
                // java.lang.Error(s) are thrown and handled by the main thread
                System.err.println("SEVERE: An error has occurred");
                e.printStackTrace();
            }
        }
    }
}
