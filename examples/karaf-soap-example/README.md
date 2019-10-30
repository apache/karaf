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
# Apache Karaf SOAP Example

## Abstract

This example shows how to use JAX-WS to implement a SOAP service.

This example uses blueprint to deal with the jaxws-server and Apache CXF as the implementation of the JAXWS specification.

It implements a `BookingService` with a SOAP WS implementation. 

The "client" bundle uses the `BookingService` with a SOAP client stub.

## Artifacts

* **karaf-soap-example-api** is a common bundle containing the `Booking` POJO and the `BookingService` interface.   
* **karaf-soap-example-blueprint** is a blueprint bundle providing the `BookingServiceSoap` implementation of the `BookingService` interface.
* **karaf-soap-example-scr** is a SCR bundle providing the `BookingServiceSoap` implementation of the `BookingService` interface.
* **karaf-soap-example-client** is a CXF client to the `BookingService` SOAP.
* **karaf-soap-example-features** provides a Karaf features repository used for the deployment.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Feature and Deployment

On a running Karaf instance, register the features repository using:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-soap-example-features/LATEST/xml
```

Then, you can install the service blueprint provider or scr provider feature:

```
karaf@root()> feature:install karaf-soap-example-blueprint
```

```
karaf@root()> feature:install karaf-soap-example-scr
```

And the service client feature:

```
karaf@root()> feature:install karaf-soap-example-client
```

## Usage

You can take a look on the WSDL generated for our SOAP WS:

```
http://localhost:8181/cxf/example?wsdl
```

The client feature installs `booking:*` commands. You can add a new booking using the `booking:add` command:

```
karaf@root()> booking:add 1 TEST TEST
```

The `booking:list` command displays the list of bookings:

```
karaf@root()> booking:list
ID | Flight | Customer
-----------------------
1  | TEST   | TEST
```

These commands use a CXF SOAP WebService client to interact with the Booking WebService.

