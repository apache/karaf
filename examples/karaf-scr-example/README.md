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
# Apache Karaf SCR example

## Abstract

SCR (Service Component Runtime) is a convenient way to use services with annotations.

As it's part of the OSGi compendium specification, you don't need any large dependency to use it.

## Artifacts

* **karaf-ds-example-api** provides the `BookingService` interface and `Booking` POJO.
* **karaf-ds-example-provider** implements and exposes a `BookingService` using Component annotations.
* **karaf-ds-example-client** uses a `BookingService` reference injected thanks to Reference annotations.
* **karaf-ds-example-features** contains a Karaf features repository used for the deployment.

## Build

Simply use Apache Maven:

```
mvn clean install
```

## Features and Deployment

On a running Karaf instance, you register the example features repository with:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-scr-example-features/LATEST/xml
```

Then, you can install the `karaf-scr-example-client` feature:

```
karaf@root()> feature:install karaf-scr-example-client
```

This feature installs the `karaf-scr-example-provider` feature providing the service, and the client bundle.

## Usage

When you install the client feature, you should see on the console:

```
karaf@root()>                                                                                                                                                                                     
-----------
1 - AF520 - John Doe

-----------
2 - AF59 - Alan Parker

```