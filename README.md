# New Relic Platform - Java SDK #

## What's new in V2? ##

For version 2 of the Java SDK, we have made several changes to help make the installation experience more uniform for plugins.  The changes include:

* 'newrelic.properties' file is now 'newrelic.json'
* Plugin configuration is now done through the 'plugin.json'
* Logging has been made more robust and easier to use.
* Jar distributables now have a well-defined name (i.e. plugin.jar)
* Configuration files are now located in a well-defined location (i.e. './config' off the root)
* EpochCounter has been deprecated and replaced with EpochProcessor
* Integration with the [New Relic Platform Installer (NPI)](https://discuss.newrelic.com/category/platform-plugins/platform-installer-beta)

More information on these changes can be found in the CHANGELOG.md file.

## Requirements ##

What you'll need to get started writing a New Relic Platform Plugin:

* Java >= 1.6
* New Relic account on http://newrelic.com
* Something to monitor


## Get Started Writing a Platform Plugin ##

The following will guide you through the process of writing a Platform plugin with our Java SDK.  For additional information, please refer to our getting started [documentation](http://newrelic.com/docs/platform/plugin-development).

### Key Concepts ###

Core classes to be aware of in the SDK:

* **Agent** - Agent is an abstract base class where most of your plugin logic should live.  When writing a plugin, you will create a class that extends Agent and overrides several key methods such as pollCycle() and getAgentName().  Each instance of an Agent should correspond to an instance of a thing you are monitoring (e.g. You may have a MySQLAgent.java file that extends Agent, so each instance of MySQLAgent would represent a different MySQL Server you are monitoring.)

* **AgentFactory** - AgentFactory is an abstract base class that is meant to help facilitate creation of Agents from the well-defined configuration file 'plugin.json'.

* **Runner** - Runner is the main entrypoint class for the SDK.  Essentially you will create an instance of a Runner, assign either your programmatically configured Agents or your AgentFactory to it, and then call setupAndRun() which will begin invoking the 'pollCycle()' method on each of your Agents once per polling interval.

* **Processors** - Processors are used to help with more complex metrics.  For example, many metrics are simply scalar values (e.g. Number of database connections, bytes read, etc) but many times you care about the metric in relation to something else such as time. We provide processors (such as the EpochProcessor) to do the heavy lifting for you.  That is, instead of just reporting 'Number of database connections' you can run that number through an EpochProcessor each poll interval and end up with 'Number of database connections per minute'.

* **Logger** - The Logger class is provided to help you easily log data within your plugin in a consistent manner to the SDK.  Simply create a new instance of a Logger using the Logger.getLogger() method and you will immediately be able provide robust logging to yourself and your plugin's consumers.

### Creating your Plugin ###

* [Step 0 - Know what you want to monitor](#step-0-know-what-you-want-to-monitor)
* [Step 1 - Create your Agent class](#step-1-create-your-agent-class)
* [Step 2 - Initialize your Agent instances](#step-2-initialize-your-agent-instances)
* [Step 3 - Setup your Agents with the Runner](#setup-your-agents-with-the-runner)
* [Step 4 - Packaging and distribution](#packaging-and-distribution)

#### Step 0 - Know what you want to monitor ####

It is very important to consider what dashboards you'd like to show for whatever it is you are monitoring in the New Relic UI before you begin development.  The reason for this is that metric names are very important when aggregating data in dashboards in the New Relic UI.  For more information, check out the following documentation site on metric naming [here](https://docs.newrelic.com/docs/plugin-dev/metric-naming-reference). 

#### Step 1 - Create your Agent class ####

Every plugin should have one class that extends the SDK's Agent class. This can be thought of as the blueprint for whatever it is you are monitoring (e.g. if you are creating a plugin to monitor MySQL instances you should create a single MySQLAgent class that extends from Agent).  There are three distinct things that must be done for each of your Agent subclasses:

* Overload the constructor
* Override the getAgentName() method
* Override the pollCycle() method

##### Overload the constructor #####

The only requirement for this step is that you pass two well-defined fields to the base SDK Agent class' constructor:

* *GUID* - This is a string field representing the unique identifier for your plugin.  We highly recommend using Java package notation (i.e. com.mycompany.pluginname)
* *Version* - This is a string field representing the version of your plugin which should follow the semantic versioning scheme (e.g. "1.2.3")

This is not a requirement, but we highly recommend overloading the constructor to take in any instance-specific fields to simplify Agent creation for Step 2.

Example:

```
    private static final String GUID = "com.yourorg.pluginname";
    private static final String VERSION = "1.0.0";
    
    public YourAgent(String name, String host, Integer port, String password) throws ConfigurationException {
        super(GUID, VERSION);
        
        this.name = name;        
        this.url = new URL(HTTP, host);
        this.port = port;
        this.password = password;
    }
```	


##### Override the getAgentName() method #####

This method determines the name that will appear in the New Relic UI for a given instance.  Continuing the MySQL analogy, you would likely use the hostname of each MySQL Server you are monitoring to make things easy to trace.

Example: 
	
    private String name;
    
    public ExampleAgent(String name, …) {
    	this.name = name;
    	…
    }
    
    @Override
    public String getAgentName() {
       	return name; 
    }

	

##### Override the pollCycle() method #####

This method will be invoked once for each Agent per polling interval while your plugin is running.  This is where your logic to gather and report metric values should live.  For each metric value you'd like to report, simply call the reportMetric() method within your pollCycle() code.  After each Agent's pollCycle() method has been invoked all reported metrics will be sent to the New Relic service in a REST request.

The reportMetric() method signature is the following:

	/**
     * Report a metric with a name, unit(s) and value.
     * The count is assumed to be 1, while minValue and maxValue are set to value.
     * Sum of squares is calculated as the value squared.
     * If the value is null, the reporting is skipped.
     * @param metricName the name of the metric
     * @param units the units to report
     * @param value the Number value to report
     */
	public void reportMetric(String metricName, String units, Number value) { … }

Example: 
	
	// Initialize processors for each metric that is processed over time
	private Processor connectionsProcessor = new EpochProcessor();;
	private Processor bytesReadProcessor = new EpochProcessor();;
	
	…
	
	@Override
    public void pollCycle() {
        int numConnections = getNumConnections();
        int bytesRead = getNumberBytesRead();
        
        // Report two metrics for each value from the server, 
        // One for the plain scalar value and one that processes the value over time
        reportMetric("Connections/Count", "connections", numConnections);
        reportMetric("Connections/Rate", "connections/sec", connectionsProcessor.process(numConnections));
        
        reportMetric("BytesRead/Count", "bytes", bytesRead);
        reportMetric("BytesRead/Rate", "bytes/sec", bytesReadProcessor.process(bytesRead));
    }

That's it, your Agent class is ready, now all you need to do is initialize them and set them up to run with the Runner!

#### Step 2 - Initialize your Agent instances ####

A plugin's value comes from the ability to dynamically configure instance information so it can be reused by others to monitor their infrastructure without code changes.  This requires instance data to come from a configuration file, which was traditionally up to the plugin developer to define.  In version 2 and later of the SDK, this information has been standardized into the 'plugin.json' file.  

##### Working with the './config/plugin.json' file #####

The 'plugin.json' file is used by all Platform Plugins to configure instance-specific information such as the hostnames you are going to monitor or the user/password combos to access them.  The file is standard JSON and only requires that you have a root-level property named "agents" that contains an array of objects.  Each object in that array should correspond to an instance of something you are monitoring.  Essentially each JSON object in the "agents" array should have all the fields necessary to initialize an instance of your Agent class created in Step 1.  You can pass whatever fields or create as many Agents as you'd like here.

Example 'plugin.json' file:

```
{
  "agents": [
    {
      "name" : "AgentName1",
      "host" : "host1.com",
      "port" : 8080
    },
    {
      "name" : "AgentName2",
      "host" : "host2.com",
      "port" : 8081
    },
    ...
  ]
}
```
##### Creating an AgentFactory class #####

Since all plugins require JSON configuration, it would be annoying for every plugin developer to have to rewrite the logic to parse a JSON file and initialize their Agents, so instead the SDK provides an AgentFactory class that you can use to automatically turn your JSON configuration into initialized Agents.  Simply extend the AgentFactory class and override the createConfiguredAgent() method.  When the SDK first starts, it will parse the 'plugin.json' file for you and map the JSON options into a map of strings to objects that you can then use to initialize and return an instance of your Agent.

Example:

```
public class ExampleAgentFactory extends AgentFactory {

    @Override
    public Agent createConfiguredAgent(Map<String, Object> properties) throws ConfigurationException {
        String name = (String) properties.get("name");
        String host = (String) properties.get("host");
        Integer port = (Integer) properties.get("port");
        
        if (name == null || host == null || port == null) {
            throw new ConfigurationException("'name', 'host', and 'port' cannot be null.");
        }
        
        return new ExampleAgent(name, host, port);
    }
}
```

#### Step 3 - Set up your Agents with the Runner ####

You're almost there! The last coding step is to create an instance of the Runner class and pass it your AgentFactory class.  This part is very simple, and is best demonstrated through a code example (since most plugins should be identical for this step):

```
public class Main {

    public static void main(String[] args) {
        try {
            Runner runner = new Runner();
            runner.add(new ExampleAgentFactory());
            runner.setupAndRun(); // Never returns
        } catch (ConfigurationException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(-1);
        }
    }
}
```

#### Step 4 - Packaging and distribution ####

Traditionally plugin developers were responsible for deciding how to distribute their plugin.  Everything from what compression format should be used to where should configuration files be located was up to each individual plugin author.  This created a serious problem for plugin consumers since every new piece of infrastructure that they wanted to monitor required reading a lot of documentation around how to set that particular plugin up.  Enter the New Relic Platform Installer (NPI) tool, a simple, light-weight command line utility that allows someone to easily download, configure and manage plugins. (Read more [here](https://discuss.newrelic.com/category/platform-plugins/platform-installer-beta)). 

In order to make your plugin NPI-compatible simply ensure the following:

* Your plugin was written with version 2 or later of the Java SDK.
* Your plugin is packaged using the tar.gz compression protocol.
* Your executable jar is named 'plugin.jar' and is located in the root of your plugin folder.
* Plugin configuration is read from 'plugin.json' file in the './config'.
* You do not use any relative references in your code.
* Your plugin contains a 'plugin.template.json' and a 'newrelic.template.json' file in the './config' directory.

Once your plugin is NPI-compatible from a code perspective, place it somewhere that is accessible for consumers to download.  Most customers add the compressed distributable to a 'dist' folder in their Github repository.  From there, go through our publisher flow and be sure to mark your plugin for "NPI" distribution.

#### Next Steps ####

That's it, you've written your first plugin.  If anything wasn't clear or you'd like a more in-depth code example, check out our [example Wikipedia plugin](https://github.com/newrelic-platform/newrelic_java_wikipedia_plugin).  

Once you're comfortable with the plugin and you're ready to set up some dashboards check out [this](https://docs.newrelic.com/docs/plugin-dev/publishing-your-plugin) link for a detailed look at the publishing process.

## Configuration Options ##

All plugins have two configuration files.  One that is standard across all plugins containing information like New Relic License Key, logging information, or proxy settings, and one that is for plugin-specific configuration options.  These configuration files both live within the `config` directory of a plugin.

### newrelic.json ###

The `newrelic.json` configuration file is where New Relic specific configuration lives. 

Example:

```
{
  "license_key": "NEW_RELIC_LICENSE_KEY"
}
```

#### Config Options ####

`license_key` - _(required)_ the New Relic license key

`log_level` - _(optional)_ the log level. Valid values: `debug`, `info`, `warn`, `error`, `fatal`. Defaults to `info`.

`log_file_name` - _(optional)_ the log file name. Defaults to `newrelic_plugin.log`.

`log_file_path` - _(optional)_ the log file path. Defaults to `logs`.

`log_limit_in_kbytes` - _(optional)_ the log file limit in kilobytes. Defaults to `25600` (25 MB). If limit is set to `0`, the log file size would not be limited.

`proxy_host` - _(optional)_ the proxy host. Ex. `webcache.example.com`

`proxy_port` - _(optional)_ the proxy port. Ex. `8080`. Defaults to `80` if a `proxy_host` is set.

`proxy_username` - _(optional)_ the proxy username

`proxy_password` - _(optional)_ the proxy password

### plugin.json ###

The `plugin.json` configuration file is where plugin specific configuration lives. A registered `AgentFactory` will receive a map of key-value pairs from within the `agents` JSON section. 

Example:

```
{
  "agents": [
    {
      "name"       : "Localhost",
      "host"       : "localhost",
      "user"       : "username",
      "password"   : "password",
      "timeout"    : 5,
      "multiplier" : 1.5
    }
  ],
  "categories": {
    "big": [1, 2, 3],
    "enabled": false
  }
}
```

### System Properties ###

The SDK also accepts the following custom JVM parameters:

* `newrelic.platform.config.dir` - Allows you to specify where your configuration files are located. (Does not currently support `~` as a home alias)

## Logging ##

The SDK provides a simple logging framework that will log to both the console and to a configurable logging file. The logging configuration is managed through the `newrelic.json` file and the available options are outlined above in the [Config Options](#config-options) section.

Example configuration:

```
{
  "log_level": "debug",
  "log_file_name": "newrelic_plugin.log",
  "log_file_path": "./path/to/logs/newrelic",
  "log_limit_in_kbytes": 1024
}
```

**Note:** All logging configuration options are optional.

Example usage:

```
import com.newrelic.metrics.publish.util.Logger;
...
private static final Logger logger = Logger.getLogger(ExampleAgent.class);
...
logger.debug("debug message");
logger.info("info message", "\tsecond message");
logger.error(new RuntimeException(), "error!");
...
```

For better visibility in logging, it is recommended to create one static `Logger` instance per class and reuse it.

## Support ##

Reach out to us at
[support.newrelic.com](http://support.newrelic.com/).
There you'll find documentation, FAQs, and forums where you can submit
suggestions and discuss with staff and other users.

Also available is community support on IRC: we generally use #newrelic
on irc.freenode.net

Find a bug?  E-mail support @  New Relic, or post it to [support.newrelic.com](http://support.newrelic.com/).

Thank you!