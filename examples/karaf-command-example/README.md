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
# Apache Karaf Shell Command example

## Abstract

This example shows how to create a shell command.

It provides a very simple command, calling a `BookingService`.

## Artifacts

* **karaf-command-example-api** is a common bundle providing the `Booking` POJO and the `BookingService` interface.
* **karaf-command-example-provider** is a very simple Karaf bundle providing `BookingService` implementation.
* **karaf-command-example-command** is the actual bundle providing the Karaf shell command.
* **karaf-command-example-features** provides a Karaf features repository used for the deployment.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Features and Deployment

On a running Karaf instance, register the features repository:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-command-example-features/LATEST/xml
```

Then, you can install the service provider feature:

```
karaf@root()> feature:install karaf-command-example-provider
```

The shell commands can be installed with the corresponding feature:

```
karaf@root()> feature:install karaf-command-example
```

## Usage

Once you have installed the feature, you can see new commands available in the Apache Karaf shell.

`booking:add` command adds a new booking in the booking service. For instance:

```
karaf@root()> booking:add AF520 Doe
```

`booking:list` command lists the current bookings:

```
karaf@root()> booking:list
ID      │ Flight │ Customer
────────┼────────┼─────────
2226065 │ AF520  │ Doe
```

`booking:remove` command removes a booking from the booking service:

```
karaf@root()> booking:remove 2226065
```