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

import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.metrics.publish.binding.Request;
import com.newrelic.metrics.publish.configuration.Config;
import com.newrelic.metrics.publish.configuration.ConfigurationException;
import com.newrelic.metrics.publish.configuration.SDKConfiguration;
import com.newrelic.metrics.publish.util.Logger;

/**
 * The main entry point for executing the SDK.
 * Add an {@link AgentFactory} to create an {@link Agent}
 * or register an {@link Agent} directly. The {@code Runner} will poll {@link Agent}s
 * in a loop that never returns.
 */
public class Runner {

    private static Logger logger;

    private List<Agent> componentAgents;
    private final SDKConfiguration config;
    private int pollInterval = 60;
    private HashSet<AgentFactory> factories = new HashSet<AgentFactory>();
    private Context context;

    /**
     * Constructs a {@code Runner}
     * @throws ConfigurationException if there is a configuration issue
     */
    public Runner() throws ConfigurationException {
        super();
        componentAgents = new LinkedList<Agent>();

        try {
            Config.init();
            Logger.init(Config.getValue("log_level", "info"),
                        Config.getValue("log_file_path", "logs"),
                        Config.getValue("log_file_name", "newrelic_plugin.log"),
                        getLogLimitInKilobytes());
            logger = Logger.getLogger(Runner.class);
            config = new SDKConfiguration();
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
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
     * Add an {@link Agent}
     * @param agent the {@link Agent} to be added
     */
    public void add(Agent agent) {
        componentAgents.add(agent);
    }

    /**
     * Register an {@link Agent}
     * @param agent the {@link Agent} to be registered
     * <p>
     * This method is now deprecated and will be removed in a future release.  Use {@link Agent#add(Agent)} instead.
     */
    @Deprecated
    public void register(Agent agent) {
        componentAgents.add(agent);
    }

    /**
     * Get the {@link SDKConfiguration} for the {@code Runner}
     * <p>
     * This class is now deprecated and will be removed in a future release. See {@link Config}.
     * @return SDKConfiguration the current {@link SDKConfiguration}
     */
    @Deprecated
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
        // TODO: when removing SDKConfiguration, move config validation here
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
            // clean up
            future.cancel(true);
            executor.shutdown();
        }
    }

    private void createAgents() throws ConfigurationException {
        for (Iterator<AgentFactory> iterator = factories.iterator(); iterator.hasNext();) {
            AgentFactory factory = (AgentFactory) iterator.next();
            factory.createConfiguredAgents(this);
        }
    }

    private void setupAgents() throws ConfigurationException {
        logger.debug("Setting up agents to be run");

        createAgents();
        if(config.internalGetServiceURI() != null) {
            logger.info("Metric service URI: ", config.internalGetServiceURI());
        }

        context = new Context();

        Iterator<Agent> iterator = componentAgents.iterator();
        while (iterator.hasNext()) {
            Agent agent = iterator.next();

            setupAgentContext(agent);

            agent.prepareToRun();
            agent.setupMetrics();

            //TODO this is a really awkward place to set the license key on the request
            context.licenseKey = config.getLicenseKey();
            if(config.internalGetServiceURI() != null) {
                context.internalSetServiceURI(config.internalGetServiceURI());
            }
            context.internalSetSSLHostVerification(config.isSSLHostVerificationEnabled());
        }
    }

    private void setupAgentContext(Agent agent) {
        // Since this data comes from the configured agents, it needs to be initialized here.  But only set it once since
        // all agents should share the same version.
        if (context.agentData.version == null) {
            context.agentData.version = agent.getVersion();
        }

        agent.getCollector().setContext(context);
        agent.getCollector().createComponent(agent.getGUID(), agent.getAgentName());
    }

    private Integer getLogLimitInKilobytes() {
        Integer logLimitInKiloBytes = 25600; // 25 MB
        if (Config.getValue("log_limit_in_kbytes") instanceof String) {
            logLimitInKiloBytes = Integer.valueOf(Config.<String>getValue("log_limit_in_kbytes"));
        }
        else if (Config.getValue("log_limit_in_kbytes") instanceof Number) {
            logLimitInKiloBytes = Config.<Long>getValue("log_limit_in_kbytes").intValue();
        }
        return logLimitInKiloBytes;
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
                logger.debug("Harvest and report data");

                Request request = context.createRequest();

                for (Iterator<Agent> iterator = componentAgents.iterator(); iterator.hasNext();) {
                    Agent agent = iterator.next();
                    agent.getCollector().setRequest(request);
                    logger.debug("Beginning poll cycle for agent: '", agent.getAgentName(), "'");
                    agent.pollCycle();
                    logger.debug("Ending poll cycle for agent: '", agent.getAgentName(), "'");
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
