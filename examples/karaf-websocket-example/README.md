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
# Apache Karaf Websocket example

## Abstract

This example show how to register a websocket in the Karaf HTTP Service.

## Artifacts

* **karaf-websocket-example** is the main bundle using `@WebSocket` annotation.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Deployment

On a running Karaf instance, first, you have to install the `http`, and `scr` features:

```
karaf@root()> feature:install http
karaf@root()> feature:install scr
```

Then, you can install the websocket example bundle:

```
karaf@root()> bundle:install -s mvn:org.apache.karaf.examples/karaf-websocket-example/LATEST
```

You can see the websocket registered in the HTTP service:

```
karaf@root()> http:list
ID │ Servlet                 │ Servlet-Name   │ State       │ Alias              │ Url
───┼─────────────────────────┼────────────────┼─────────────┼────────────────────┼───────────────────────
92 │ WebsocketExampleServlet │ ServletModel-2 │ Deployed    │ /example-websocket │ [/example-websocket/*]
```

## Usage

The websocket is available on http://localhost:8181/example-websocket.

You can now register your client (like `curl`) on this URL:

```
curl --include \
     --no-buffer \
     --header "Connection: Upgrade" \
     --header "Upgrade: websocket" \
     --header "Host: localhost:8181" \
     --header "Origin: http://localhost:8181/example-websocket" \
     --header "Sec-WebSocket-Key: SGVsbG8sIHdvcmxkIQ==" \
     --header "Sec-WebSocket-Version: 13" \
     http://localhost:8181/example-websocket
```

Your client will have a `Hello World` message on your client every second.