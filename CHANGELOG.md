## New Relic Platform Java SDK Change Log ##

### v1.2.1 - Unreleased ###

**Bug Fixes**

* Fixed logic around EpochCounters processing null values. 

### v1.2.0 - October 29, 2013 ###

**Improvements**

* Added ability to consolidate sending multiple component agent metrics in a single REST request per poll cycle.

### v1.1.2 - October 22, 2013 ###

**Bug Fixes**

* Increased accuracy for timestamps in calculating component durations.

**Improvements**

* Added SDK lifecycle logging enhancements.

### v1.1.1 - September 4, 2013 ###

**Features**

* Added 20 minute aggregation limit for when an agent cannot connect to New Relic's Metric API.

### v1.1.0 - August 19, 2013 ###

**Features**

* Added support for metric aggregation when the agent cannot connect to New Relic's Metric API.

### v1.0.4 - July 24, 2013 ###

**Features**

* Added support for automatic handling of component durations between successful metric publishes.

### v1.0.3 - July 18, 2013 ###

**Bug Fixes**

* Fixed `EpochCounter#process` issue where sub-second intervals (e.g. 400 ms) would return non-null values.

### v1.0.2 - July 7, 2013 ###

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