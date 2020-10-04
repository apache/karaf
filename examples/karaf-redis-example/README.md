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
# Apache Karaf Redis Example

This example shows how to use Redis as data storage.

As for other examples, it implements `BookingService` using Redis as storage.

## Artifacts

* **karaf-example-redis-api** provides `BookingService` interface and `Booking` POJO.
* **karaf-example-redis-service** implements `BookingService` using Redis as storage.
* **karaf-example-redis-command** provides shell commands which use the `BookingService`.
* **karaf-example-redis-features** provides the Karaf features repository used for the deployment.

## Build

The build uses Apache Maven. Simply use:

```
$ mvn clean install
```

## Features and Deployment

On a running Karaf instance, register the features repository using:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-example-redis-features/LATEST/xml
```

Then you can install the Redis service and command features:

```
karaf@root()> feature:install karaf-example-redis-service
karaf@root()> feature:install karaf-example-redis-command
```

## Usage

Once you have installed the features, you can see new commands available in the Apache Karaf shell.

`booking:add` command adds a new booking in the booking service. For instance:

```
karaf@root()> booking:add Doe AF520
```

`booking:list` command lists the current bookings:

```
karaf@root()> booking:list
ID      │ Flight │ Customer
────────┼────────┼─────────
1       │ AF520  │ Doe
```