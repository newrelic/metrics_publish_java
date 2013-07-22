## New Relic Platform Java SDK Change Log ##

### v1.0.4 - UNRELEASED ###

**Features**

* Added support for automatic handling of component durations between successful metric publishes.

### v1.0.3 - July 18th, 2013 ###

**Bug Fixes**

* Fixed `EpochCounter#process` issue where sub-second intervals (e.g. 400 ms) would return non-null values.

### v1.0.2 - July 7th, 2013 ###

**Bug Fixes**

* Fixed an issue where component data was being benignly duplicated in the request's JSON.

### v1.0.1 - June 24, 2013 ###

**Features**

* Improved logging for readability
* Added configuration option for SSL hostname verification
  
**Changes**

* No breaking changes

### v1.0.0 - June 18, 2013 ###

**Features**

* Initial release version of the New Relic Java SDK
  * Ability to create Agents for reporting metrics by subclassing `Agent`
  * Ability to configure Agents via an `AgentFactory` with a JSON configuration file
  * Ability to process metrics by time interval with an `EpochCounter`
  
**Changes**

* `EpochCounter` processor will now return `null` instead of 0 for invalid data points
* `Agent#reportMetric(String, String, int)` and `Agent#reportMetric(String, String, float)` have been consolidated to `Agent#reportMetric(String, String, Number)`