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
# Apache Karaf Diagnostic Dump example

## Abstract

This example shows how to create a new dump provider service. This provider is automatically included in the dump created
by the `dev:dump-create` command.

The dump provider example actually takes a screenshot of client screen.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Deployment

On a running Karaf instance, install the scr feature and the dump bundle:

```
karaf@root()> feature:install scr
karaf@root()> bundle:install -s mvn:org.apache.karaf.examples/karaf-dump-example/LATEST
```

## Usage

Once you have installed the bundle, you can take a dump using:

```
karaf@root()> dev:dump-create
```

You will find the dump zip file in the Karaf home folder. If you extract this zip, in addition of the regular files (heap dump, thread dump, ...),
you will see screenshot files created by our example dump provider:

```
Archive:  xxxx-xx-xx_xxxxxx.zip
  inflating: environment.txt         
  inflating: memory.txt              
  inflating: threads.txt             
  inflating: heapdump.txt            
  inflating: bundles.txt             
  inflating: features.txt            
  inflating: screenshot/display_0.png  
  inflating: screenshot/display_1.png  
  inflating: log/security.log        
  inflating: log/karaf.log  
```
