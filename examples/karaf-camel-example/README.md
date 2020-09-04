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
# Apache Karaf Camel Example

## Abstract

This example shows how to use Apache Camel in Karaf. Apache Camel is a integration framework, allowing you to integrate several systems and applications all together.

Apache Camel supports several DSL. This example shows how to use the Camel Java DSL and the Camel Blueprint DSL.

It creates several Camel routes, exposing HTTP endpoint and using a Content Based Router EIP (Enterprise Integration Pattern).

## Artifacts

* **karaf-camel-example-java** is a bundle containing routes described using the Camel Java DSL loaded by SCR.
* **karaf-camel-example-blueprint** is just a wrapper containing routes described using Blueprint. Karaf supports deployment of this DSL directly (in the deploy folder for instance) or packaged as a bundle.
* **karaf-camel-example-features** provides a Karaf features repository used for the deployment.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Feature and Deployment

On a running Karaf instance, register the features repository using:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-camel-example-features/LATEST/xml
```

Then, you can install either Camel Java or Blueprint features:

```
karaf@root()> feature:install karaf-camel-example-java
```

```
karaf@root()> feature:install karaf-camel-example-blueprint
```

## Usage

Once you have installed a Camel feature, the main route started and bind HTTP endpoint on `http://localhost:9090/example`.

We can test payloads testing different paths of the content based router.

First, let's try an e-mail request:

```
curl -X POST -H "Content-Type: application/json" http://localhost:9090/example -d '{ "notification": { "type": "email", "to": "foo@bar.com", "message": "This is a test" }}'
```

You can see in the log that the e-mail route has been called in the log:

```
karaf@root()> log:display
10:24:22.453 INFO [qtp1790923892-140] [EXAMPLE INBOUND] Received: { "notification": { "type": "email", "to": "foo@bar.com", "message": "This is a test" }}
10:24:22.465 INFO [qtp1790923892-140] [EXAMPLE INBOUND] Received email notification
10:24:22.466 INFO [qtp1790923892-140] [EXAMPLE EMAIL] Sending notification email
```

And we have the curl response confirming it:

```
{ "status": "email sent", "to": "foo@bar.com", "subject": "Notification"}
```

We can also test the `http` path of the content based router:

```
curl -X POST -H "Content-Type: application/json" http://localhost:9090/example -d '{ "notification": { "type": "http", "service": "http://foo" }}'
```

We can see the response confirming the call to the http route path:

```
{ "status": "http requested", "service": "http://foo" }
```

and the corresponding log messages:

```
11:17:28.372 INFO [qtp1790923892-138] [EXAMPLE INBOUND] Received: { "notification": { "type": "http", "service": "http://foo" }}
11:17:28.374 INFO [qtp1790923892-138] [EXAMPLE INBOUND] Received http notification
11:17:28.374 INFO [qtp1790923892-138] [EXAMPLE HTTP] Sending http notification
```