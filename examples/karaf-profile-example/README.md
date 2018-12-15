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
# Apache Karaf Profile example

## Abstract

This example shows you how to create several profiles (in a registry) and use these profiles to create custom distributions.

## Artifacts

* **karaf-profile-example-registry** is a main container for profiles (registry).
* **karaf-profile-example-dynamic** is a custom Karaf distribution where we apply profiles "on the fly".
* **karaf-profile-example-static** is a custom Karaf distribution "static", for instance, very convenient to run on Docker.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Registry

The `karaf-profile-example-registry` module contains several profiles (with inheritance). A profile is described with a `profile.cfg` and contains
resources used in the Karaf container (cfg file, bundles, features, ...).

You can build the registry simply using:

```
mvn clean install
```

## Assembly & Distributions

### Dynamic

The `karaf-profile-example-dynamic` module uses the profile registry (using `<profileUri/>`) to create an assembly applying profiles as boot profiles:

* `karaf`
* `loanbroker-bank1`
* `loanbroker-bank2`
* `loanbroker-bank3`
* `loanbroker-broker`
* `activemq-broker`

To build the distribution, simply do:

```
mvn clean install
```

You can find the resulting distribution in the `target` folder: `karaf-profile-example-dynamic*.tar.gz`.

### Static

The `karaf-profile-example-static` module uses also the profiles to create a distribution using the same profiles, however, it uses a static approach. It's especially
designed to run a docker or a simple JVM without dynamic change of configuration.

The build the distribution, simply do:

```
mvn clean install
```

You can find the resulting distribution in the `target` folder: `karaf-profile-example-static-xxxx.tar.gz`.

### Profile & shell

On a running Karaf instance, you can install the `profile` feature using:

```
karaf@root()> feature:install profile
```

Then, you have new commands available in the shell, allowing you to create, edit, apply profiles.
