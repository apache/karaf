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
# Apache Karaf Servlet examples

## Abstract

This example show how to register a servlet in the Karaf HTTP Service.

It uses different approaches:

1. Using `@WebServlet` annotation using the Karaf annotation scanner
2. Registering a servlet directly into the Karaf HTTP service
3. Using Karaf Servlet whiteboard pattern with blueprint
4. Using Karaf Servlet whiteboard pattern with SCR

## Artifacts

* **karaf-servlet-example-annotation** is a bundle using `@WebServlet` Servlet 3.0 annotation to register.
* **karaf-servlet-example-registration** is a bundle that register a servlet directly into the Karaf HTTP service.
* **karaf-servlet-example-blueprint** is a Blueprint bundle registering a Servlet service used by the Karaf Servlet whiteboard pattern.
* **karaf-servlet-example-scr** is a SCR bundle registering a Servlet service used by the Karaf Servlet whiteboard pattern.
* **karaf-servlet-example-features** provides a Karaf features repository used for the deployment.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Features and Deployment

On a running Karaf instance, you register the example features repository with:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-servlet-example-features/LATEST/xml
```

As requirement, you need to install a HTTP service provider (for instance `felix-http` feature or `http` (Pax Web) feature).
So either:

```
karaf@root()> feature:install http
karaf@root()> feature:install http-whiteboard
```

or

```
karaf@root()> feature:install felix-http
```

The `karaf-servlet-example-annotation` feature installs the required features (HTTP service) and register the servlet using
Servlet 3.0 annotation:

```
karaf@root()> feature:install karaf-servlet-example-annotation
```

The `karaf-servlet-example-registration` feature installs the required features (HTTP service) and register the servlet by hand in the Karaf HTTP service:

```
karaf@root()> feature:install karaf-servlet-example-registration
```

The `karaf-servlet-example-blueprint` feature installs the required features (HTTP service & blueprint) and register the servlet using
a Servlet service:

```
karaf@root()> feature:install karaf-servlet-example-blueprint
```

The `karaf-servlet-example-scr` feature installs the required features (HTTP service & SCR) and register the servlet using
a Servlet service:

```
karaf@root()> feature:install karaf-servlet-example-scr
```

## Usage

Whatever feature you use, you can access the servlet on the following URL:

[http://localhost:8181/servlet-example]

## Upload Servlet

You can also find a upload servlet example using multipart data.

You can install it with:

```
karaf@root()> feature:install karaf-servlet-example-upload
```

Then, you can use `curl` to upload data via this servlet:

```
curl --progress-bar -v -k -F file=/my/file http://127.0.0.1:8181/upload-example
```