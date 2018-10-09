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
# Apache Karaf Jersey REST API Example

## Abstract

This is a small apache karaf demo application that demonstrates how to use Jersey in Apache Karaf, and how to inject OSGi services into Jersey resources.

## Artifacts

* `karaf-jersey-example.servicedef` contains an interface defining an OSGi service
* `karaf-jersey-example.services` implements the service with a DS component
* `karaf-jersey-example.webapi` contains a web whiteboard DS component that implements a Servlet service based on the Jersey servlet container.  In that DS component, it does a little magic to connect the Jersey resources to OSGi services.  Basically the CounterServiceServlet.init() method makes it possible to have the OSGi services injected into the Jersey resources when Jersey creates them
* `karaf-jersey-example.webgui` contains a DS component that creates a web whiteboard Servlet service that defines a simple web GUI for the counter service

The top POM also creates and attaches a feature.xml file including all of the bundles' feature repositories.

## How to build the example

Build the example using the command:

```
mvn clean install
```

## How to deploy the example

In the apache karaf command line, install the features that pulls in the servlets with their dependencies:

```
feature:repo-add mvn:org.apache.karaf.examples/karaf-jersey-example/LATEST/xml/features
feature:install karaf-jersey-example.webgui
```

If karaf was started locally, try opening http://localhost:8181/karaf-jersey-example in a web browser to run the application.

The Jersey REST endpoint will be running at http://localhost:8181/karaf-jersey-example/api/counter and will be responding to both GET and POST.
