# New Relic Platform Agent SDK #

## What's new in V2? ##

For version 2 of the Java SDK, we have made several changes to help make the installation experience more uniform for plugins.  The changes include:

* 'newrelic.properties' file is now 'newrelic.json'
* Plugin configuration is now done through the 'plugin.json'
* Logging has been made more robust and easier to use.
* Jar distributables now have a well-defined name (i.e. plugin.jar)
* Configuration files are now located in a well-defined location (i.e. './config' off the root)

More information on these changes can be found in the CHANGELOG.md file.

## Requirements ##

* Java >= 1.6
* New Relic account on http://newrelic.com

## Get Started ##

This repo represents the New Relic Java SDK used to build plugin agents for the New Relic platform. If you are looking to build or use a platform plugin, please refer to the getting started [documentation](http://newrelic.com/docs/platform/plugin-development).

Add the New Relic Java SDK to your plugin's classpath to start developing.

## Configuration ##

Configuration files live within the `config` directory.

### New Relic ###

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

### Plugin ###

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

## System Properties ##

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