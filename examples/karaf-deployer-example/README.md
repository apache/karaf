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
# Apache Karaf Deployer example

## Abstract

This example shows how to create a Karaf deployer.

Karaf uses deployer services on the `deploy` folder (using Felix FileInstall). For each file in the `deploy`, Karaf calls
each deployer service to know:

1. If the deployer service should handle the file (based on file contain, file name, ...)
2. Actually let the deployer service deploys the file.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Deployment

On a running Karaf instance, just install the deployer service using:

```
karaf@root()> feature:install scr
karaf@root()> bundle:install -s mvn:org.apache.karaf.examples/karaf-deployer-example/LATEST
```


## Usage

Once deployed, you can drop any file in the `deploy` folder, you should see (for instance, if you dropped a file name `TEST` in the `deploy` folder):

```
Example deployer should install TEST
```

If you change the file, you should see:

```
Example deployer should update TEST
```

And if you remove the file from the `deploy` folder, you should see:

```
Example deployer should uninstall TEST
```