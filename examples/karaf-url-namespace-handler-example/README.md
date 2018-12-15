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
# Apache Karaf URL Namespace Handler Example

## Abstract

This example shows how to create a new URL namespace handler and use it in all Apache Karaf parts.

## Artifacts

* **karaf-url-namespace-handler-example-core** is the core bundle providing the URL handler.
* **karaf-url-namespace-handler-example-features** contains the features repository used for deployment.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Features and Deployment

On a running Karaf instance, register the features repository:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-url-namespace-handler-example-features/LATEST/xml
```

Then, you can install the `karaf-url-namespace-handler-example` feature:

```
karaf@root()> feature:install karaf-url-namespace-handler-example
```

## Usage

Once you have installed the feature, you can use URL like `example:*` wrapping any already supported URL. When you use
the `example:*` URL, a greeting message will be displayed.

For instance:

```
karaf@root()> bundle:install example:mvn:commons-lang/commons-lang/2.6
Thanks for using the Example URL !
Bundle ID: 44
```