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
# Apache Karaf JMS Example

## Abstract

The JMS section of the user guide shows how to create connection factories and manipulate JMS using the Apache Karaf provided `jms:*` commands and related.

This example shows how to use the JMS Karaf feature from a developer perspective. It directly uses a JMS `ConnectionFactory` service in code that you can implement to interact with JMS.

## Artifacts

* **karaf-jms-example-command** is a command bundle using JMS code with a `ConnectionFactory` service.
* **karaf-jms-example-features** is a features repository used for the deployment.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Features and Deployment

On a running Karaf instance, register the features repository:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-jms-example-features/LATEST/xml
```

Then, you can install the `karaf-jms-example-activemq` feature:

```
karaf@root()> feature:install karaf-jms-example-activemq
```

It will install an Apache ActiveMQ in your running Karaf instance, providing a complete embedded ActiveMQ broker, listening on port 61616 by default.

Then, you can install the `karaf-jms-example-connectionfactory` feature:

```
karaf@root()> feature:install karaf-jms-example-connectionfactory
```

This feature is the "client" part: it uses the Karaf `jms` feature and related (like `pax-jms`) to create a `ConnectionFactory` service to the embedded ActiveMQ instance.

Finally, the `karaf-jms-example-command` feature installs a shell commands bundle:

```
karaf@root()> feature:install karaf-jms-example-command
```

## Usage

The `karaf-jms-example-command` feature installed a bundle providing `example:*` commands.

You can use the `example:send` command to send a message to the ActiveMQ broker:

```
karaf@root()> example:send TEST FOO
```

where `TEST` is the JMS queue name and `FOO` the JMS message payload.

Then, you can consume the latest message in a queue using `example:consume`:

```
karaf@root()> example:consume TEST
```