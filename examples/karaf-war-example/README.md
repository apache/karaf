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
# Apache Karaf WAR Example

## Abstract

As a polymorphic runtime, Apache Karaf is able to deploy and run lot of different kind of applications.

It's especially possible to deploy a web application packaged as a war, as you can do in any web container.

This example shows how to create a regular war to be deployed in Apache Karaf.

## Artifacts

* `karaf-war-example-webapp` is a regular simple war.
* `karaf-war-example-features` provides a Karaf features repository used for deployment.

## Build 

Simply use:

```
mvn clean install
```

## Feature and Deployment

