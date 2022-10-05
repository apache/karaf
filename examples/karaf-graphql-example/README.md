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
# Apache Karaf GraphQL example

## Abstract

This example shows how to use GraphQL in Karaf. 
We demonstrate how to use GraphQL with WebSockets, with a HTTP servlet and with Karaf commands.

## Build
The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Deployment

On a running Karaf instance, add a feature repository and then the feature:
```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-graphql-example-features/LATEST/xml
karaf@root()> feature:install karaf-graphql-example 
```

## Usage
A HTTP server will start on the configured port in Karaf (8181 by default).
The following endpoints can be used to test the GraphQL API:
```
GET http://localhost:8181/graphql?query={ bookById(id:"1") { name }}
GET http://localhost:8181/graphql?query={ bookById(id:"2") { name  id authorId pageCount}}

POST http://localhost:8181/graphql
{
  "query": "mutation { addBook(name:\"Test\", pageCount:100) { name } }"
}
```

Additionally, a `graphql:query` command will be available. It takes a single mandatory argument
which needs to be a valid GraphQL query in the defined GraphQL schema.
For instance:
```
karaf@root()> graphql:query "{books { name id pageCount }}"
{books=[{name=Apache Karaf Cookbook, id=1, pageCount=260}, {name=Effective Java, id=2, pageCount=416}, {name=OSGi in Action, id=3, pageCount=375}]}

karaf@root()> graphql:query "{bookById(id:1) { name id pageCount }}"                                                                                                                  
{bookById={name=Apache Karaf Cookbook, id=1, pageCount=260}}

karaf@root()> graphql:query "mutation { addBook(name:\"Lord of the Rings\" pageCount:100) { id name }}"
{addBook={id=9, name=Lord of the Rings}}
```

Finally, the `karaf-graphql-example-websocket` bundle contains a WebSocket endpoint that will publish updates
when new data is added via GraphQL. To test, execute the following cURL command:
```
curl --include \
     --no-buffer \
     --header "Connection: Upgrade" \
     --header "Upgrade: websocket" \
     --header "Host: localhost:8181" \
     --header "Origin: http://localhost:8181/graphql-websocket" \
     --header "Sec-WebSocket-Key: SGVsbG8sIHdvcmxkIQ==" \
     --header "Sec-WebSocket-Version: 13" \
     http://localhost:8181/graphql-websocket
```
You should see a similar response:
```
HTTP/1.1 101 Switching Protocols
Date: Tue, 04 Oct 2022 21:07:55 GMT
Connection: Upgrade
Sec-WebSocket-Accept: qGEgH3En71di5rrssAZTmtRTyFk=
Upgrade: WebSocket

```

Add a new book by with the GraphQL mutation (either with a POST request or Karaf command):
```
karaf@root()> graphql:query "mutation { addBook(name:\"Lord of the Rings\" pageCount:123) { id name }}"                                                                               
```
and observe the update come in real time to the terminal with cURL:
```
{bookCreated={id=6, name=Lord of the Rings}}
```