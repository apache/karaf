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
# Apache Karaf HTTP Resource Example

## Abstract

Apache Karaf supports HTTP resources artifact. It's very simple bundle that just register an empty resource service.

The actual resources (images, static HTML pages, ...) are in a folder in the bundle.
The resource service properties configures the location of the resource and the HTTP context (pattern or HTTP location) where to expose these resources.

## Artifacts

* **karaf-http-resource-example-whiteboard** is a HTTP resource bundle that use an empty resource service (using SCR) to be taken by the Karaf HTTP service whiteboard.
* **karaf-http-resource-example-features** is a features repository used for the deployment.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Features and Deployment

On a running Karaf instance, register the features repository:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-http-resource-example-features/LATEST/xml
```

Then, you can install the `karaf-http-resource-example-whiteboard` feature:

```
karaf@root()> feature:install karaf-http-resource-example-whiteboard
```

## Usage

The `karaf-http-resource-example-whiteboard` registers HTTP resource pattern.

You can access the resources using your browser on http://localhost:8181/example/index.html URL.

Optionally, you can install `pax-web-karaf` feature to get `web:servlet-list` command and see the `ReesourceServlet` deployed:

```
karaf@root()> feature:install pax-web-karaf
```

then you can use `web:servlet-list` command:

```
karaf@root()> web:servlet-list 
Bundle ID │ Name                                         │ Class                                                             │ Context Path(s) │ URLs       │ Type       │ Context Filter
──────────┼──────────────────────────────────────────────┼───────────────────────────────────────────────────────────────────┼─────────────────┼────────────┼────────────┼────────────────────────────────────────────
55        │ default-a60a5bcb-236b-4db5-b98f-41bf3845a7cd │ org.ops4j.pax.web.service.jetty.internal.web.JettyResourceServlet │ /               │ /example/* │ Whiteboard │ (osgi.http.whiteboard.context.name=default)
```