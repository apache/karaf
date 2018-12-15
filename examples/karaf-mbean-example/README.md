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
# Apache Karaf MBean example

## Abstract

This example shows different ways to register a JMX MBean in the Apache Karaf MBeanServer.

It exposes a MBean interacting with a `BookingService`.

## Artifacts

* **karaf-mbean-example-api** is a common bundle providing the `Booking` POJO and a `BookingService` interface.
* **karaf-mbean-example-provider** is a very simple bundle providing a `BookingService` implementation.
* **karaf-mbean-example-simple** is a bundle doing a MBean registration using the Karaf util
* **karaf-mbean-example-blueprint** is a bundle doing a MBean registration using blueprint
* **karaf-mbean-example-scr** is a bundle doing a MBean registration using SCR

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Features and Deployment

On a running Karaf instance, register the features repository:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-mbean-example-features/LATEST/xml
```

Then you can install the service provider feature:

```
karaf@root()> feature:install karaf-mbean-example-provider
```

Then you can test the different registration approach:

* To use the Karaf util registration, you install the `karaf-mbean-example-simple` feature:

```
karaf@root()> feature:install karaf-mbean-example-simple
```

* To use the blueprint registration, you install the `karaf-mbean-example-blueprint` feature:

```
karaf@root()> feature:install karaf-mbean-example-blueprint
``` 

* To use the SCR registration, you install the `karaf-mbean-example-scr` feature:

```
karaf@root()> feature:install karaf-mbean-example-scr
```  

## Usage

Once you have installed a feature registering the MBean, you can see the MBean using any JMX client, like `jconsole`. 