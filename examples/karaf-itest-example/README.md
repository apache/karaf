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
# Apache Karaf Integration Test example

## Abstract

This example shows how to create integration tests.

In addition of unit tests you do in each bundles, you can easily implement integration test in Karaf.

The life cycle is basically:

1. Download and extract any Apache Karaf version.
2. Start the Karaf instance (eventually overwriting some configuration).
3. Execution some actions on this running instance (executing shell commandes, installing features, ...).
4. Verify if the instance state is the expected one.

This simple example shows how to extend `KarafTestSupport` and implements a test performing shell commands and verify
the results.

## Artifacts

The example itself is a Maven project performing the tests.

## Build & Executing Tests

To test, simply use:

```
mvn clean test
```