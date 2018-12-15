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
# Apache Karaf JPA Example

## Abstract

This example shows how to use JPA with an entity. The entity manager is used in the persistence implementation.

It implements a `BookingService` similar using a database for the storage, with a JPA entity.

This example uses blueprint and declarative service to deal with JPA entity manager.

The "command" bundle uses the `BookingService`.

## Artifacts

* **karaf-jpa-example-provider** module contain:
    * **karaf-jpa-example-api** is a bundle providing the `Booking` entity used in the `BookingService`. As a best practice, this bundle should use a common bundle containing
the `BookingService` interface, and then wrapping `Booking` POJO as a `JpaBooking` entity for instance. For convenience and reduce the number of
example artifacts, we gather interface and implementation in the same bundle (again, it's bad).
    * **karaf-jpa-example-provider-blueprint** contain bundles providing EclipseLink and Hibernate implementation using blueprint.
    * **karaf-jpa-example-provider-ds** contain bundles providing EclipseLink and Hibernate implementation using declarative service.
* **karaf-jpa-example-command** provides shell command to manipulate the `BookingService`. It use declarative service to retreive one instance of a `BookingService`. 
If you want to use several service provider, you have to stop the actual service and install the new one.
* **karaf-jpa-example-features** provides a Karaf features repository used for the deployment.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Feature and Deployment

On a running Karaf instance, register the features repository using:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-jpa-example-features/LATEST/xml
```

Then, you can install the datasource feature:

```
karaf@root()> feature:install karaf-jpa-example-datasource
```

Then, you can install the service command feature (commands booking:xxx will not be available until you have installed 
a provider):

```
karaf@root()> feature:install karaf-jpa-example-command
```

And install the service provider you want to use (for example EclipseLink using declarative service):

```
karaf@root()> feature:install karaf-jpa-example-provider-ds-eclipselink
```

## Usage

Once you have installed the feature, you can see new commands available in the Apache Karaf shell.

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

`booking:get` command get the booking with id:

```
karaf@root()> booking:get 1
ID      │ Flight │ Customer
────────┼────────┼─────────
1       │ AF520  │ Doe
```

`booking:remove` command removes a booking from the booking service:

```
karaf@root()> booking:remove 1
```