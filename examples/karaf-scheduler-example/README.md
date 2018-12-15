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
# Apache Karaf Scheduler example

## Abstract

This example shows you how to create a runnable service periodically executed by the Apache Karaf scheduler.

More than executing periodically shell commands, the Karaf Scheduler uses whiteboard pattern to automatically schedule
runnable services.

In this example, we will register a Runnable service displaying a message in the shell console. As service properties, we 
add details for the scheduler to periodically call our service.

## Artifacts

* **karaf-scheduler-example-runnable** is the core Runnable service with the scheduler details.
* **karaf-scheduler-example-features** contains features used for deployment.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Features & Deployment

On a running Karaf instance, register the features repository using:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-scheduler-example-features/LATEST/xml
```

Then, you can install the runnable service feature:

```
karaf@root()> feature:install karaf-scheduler-example
```

## Usage

Once you have installed the runnable feature, you will periodically see this kind of message in the shell console:

```
Hello Karaf User !
```

You can see the task in the Karaf scheduler using the `scheduler:list` command:

```
karaf@root()> scheduler:list 
Name        │ Schedule
────────────┼──────────────────────────────────────────
example.116 │ at(2018-02-19T11:07:24.829+01:00, -1, 10)
```
