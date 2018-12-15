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
# Apache Karaf Bundle Example

## Abstract

One of the kind of applications you can deploy in Apache Karaf is obviously OSGi bundle.

The bundle is the core deployment unit when using OSGi. It's basically a regular jar file containing some additional headers in the MANIFEST used by the OSGi framework, and so Karaf.

## Artifacts

* `karaf-bundle-example-common` provides the `BookingService` interface and `Booking` POJO.
* `karaf-bundle-example-provider` implements and exposes a `BookingService` in the OSGi service registry.
* `karaf-bundle-example-client` exposes a new `ClientService` using the `BookingService`. This service is used to periodically manipulate the `BookingService`.
* `karaf-bundle-example-features` contains a Karaf features repository used for the deployment.

## Build 

Simply use:

```
mvn clean install
```

## Feature and Deployment

On a running Karaf instance, you register the bundle example features repository with:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-bundle-example-features/LATEST/xml
```

Then you can install the `karaf-bundle-example-provider` feature:

```
karaf@root()> feature:install karaf-bundle-example-provider
```

Now, you can install the `karaf-bundle-example-client` feature:

```
karaf@root()> feature:install karaf-bundle-example-client
```

When you install the client feature, you should see on the console:

```
karaf@root()> 1794197511025182174 | John Doo | AF3030
```
