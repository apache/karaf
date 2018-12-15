<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->
# Apache Karaf Pax Logging Appender example

## Abstract

This example shows how to register a custom Pax Logging appender. It allows you to receive any log events happening
internally in Karaf, and push the content wherever you want.
As usual in Apache Karaf, all is dynamic, meaning that you can add or remove Pax Logging appender on the fly.

## Artifacts

* **karaf-log-appender-example-core** is a bundle registering a `PaxAppender` service. This service is simple: just display the log message in stdout.
* **karaf-log-appender-example-features** contains Karaf features used for deployment

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Features and Deployment

On a running Karaf instance, register the features repository:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-log-appender-example-features/LATEST/xml
```

Then you can install the log appender feature:

```
karaf@root()> feature:install karaf-log-appender-example
```

## Usage

Once you have installed the `karaf-log-appender-example` feature, all log messages will be displayed in stdout (the Apache Karaf shell console).

You can test this behavior by creating a log message using the `log:log` command:

```
karaf@root()> log:log Test
INFO - Test
```