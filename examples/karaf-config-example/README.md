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
# Apache Karaf Configuration Example

## Abstract

This example shows how to use configuration in your application, introducing different approach:

* the static approach using `ConfigurationAdmin` service, where the configuration is loaded once, on demand.
* the `ManagedService` approach which is dynamic. Your application receives a notification when the configuration changes.
* the `ManagedServiceFactory` dynamic approach that can handle several configurations created.
* the `ConfigurationListener` approach is able to listen for any change in all configurations.
* the `blueprint` approach is similar to `Managed` dynamic approach using blueprint
* the `scr` approach is similar to `Managed` dynamic approach using scr

During the installation of a configuration example feature, a configuration is created with the `org.apache.karaf.example.config` persistent id.
The configuration uses `etc/org.apache.karaf.example.config.cfg` configuration file.

## Artifacts

* **karaf-config-example-static** uses the `ConfigurationAdmin` service to retrieve the configuration identified by a PID (Persistent ID).
* **karaf-config-example-managed** uses the dynamic approach exposing a `Managed` service.
* **karaf-config-example-managed-factory** dealing with several configurations created using a `ManagedFactory` service.
* **karaf-config-example-listener** listens for any change in any configuration.
* **karaf-config-example-blueprint** uses configuration within a blueprint container.
* **karaf-config-example-scr** uses configuration within a scr component.
* **karaf-config-example-features** contains a Apache Karaf features repository used for deployment.

## Build

The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Feature and Deployment

On a running Karaf instance, register the features repository using:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-config-example-features/4.2.3-SNAPSHOT/xml
```

## Usage

### Static

If you install the `karaf-config-example-static` feature, it will create the `etc/org.apache.karaf.example.config.cfg` configuration file with the
`org.apache.karaf.example.config` configuration. At startup, the `karaf-config-example-static` feature will display the content of the configuration:

```
karaf@root()> feature:install karaf-config-example-static 
felix.fileinstall.filename = file:/home/jbonofre/Workspace/karaf/assemblies/apache-karaf/target/apache-karaf-4.2.3-SNAPSHOT/etc/org.apache.karaf.example.config.cfg
foo = bar
hello = world
org.apache.karaf.features.configKey = org.apache.karaf.example.config
service.pid = org.apache.karaf.example.config
```

### Managed

You can also test the dynamic approach using `karaf-config-example-managed` feature:

```
karaf@root()> feature:install karaf-config-example-managed
```

Then, you can install the `karaf-config-example-common` feature providing the configuration. Then, you can see the managed service called:

```
karaf@root()> feature:install karaf-config-example-common
Configuration changed
felix.fileinstall.filename = file:/home/jbonofre/Workspace/karaf/assemblies/apache-karaf/target/apache-karaf-4.2.3-SNAPSHOT/etc/org.apache.karaf.example.config.cfg
foo = bar
org.apache.karaf.features.configKey = org.apache.karaf.example.config
service.pid = org.apache.karaf.example.config
```

If you change the configuration (using `config:edit` command), you can see also the managed service called:

```
karaf@root()> config:edit org.apache.karaf.example.config
karaf@root()> config:property-set hello world
karaf@root()> config:update
Configuration changed
felix.fileinstall.filename = file:/home/jbonofre/Workspace/karaf/assemblies/apache-karaf/target/apache-karaf-4.2.3-SNAPSHOT/etc/org.apache.karaf.example.config.cfg
foo = bar
hello = world
org.apache.karaf.features.configKey = org.apache.karaf.example.config
service.pid = org.apache.karaf.example.config
```

### Managed factory

The managed service factory approach allows you to deal with several configurations of the same kind. The "base" pid is `org.apache.karaf.example.config`, then you ca
create a new configuration based on this one. For instance, you can create `etc/org.apache.karaf.example.config-example.cfg` containing:

```
hello=world
```

Then you will see in the Karaf shell console:

```
karaf@root()> New configuration with pid org.apache.karaf.example.config.994408d3-b950-4ef5-9cf0-eaaad97922f3
felix.fileinstall.filename = file:/home/jbonofre/Workspace/karaf/assemblies/apache-karaf/target/apache-karaf-4.2.3-SNAPSHOT/etc/org.apache.karaf.example.config-test.cfg
hello = world
service.factoryPid = org.apache.karaf.example.config
service.pid = org.apache.karaf.example.config.994408d3-b950-4ef5-9cf0-eaaad97922f3
```

If you remove `etc/org.apache.karaf.example.config-example.cfg` file, you will see:

```
Delete configuration with pid org.apache.karaf.example.config.994408d3-b950-4ef5-9cf0-eaaad97922f3
```

### Configuration listener

The `karaf-config-example-listener` feature installs a configuration listener:

```
karaf@root()> feature:install karaf-config-example-listener
```

Then you can create `etc/my.config.cfg` configuration file, you will see:

```
Configuration my.config has been updated
```

If you delete `etc/my.config.cfg` configuration file, you will see:

```
Configuration my.config has been deleted
```

### Blueprint

Apache Aries Blueprint provides Blueprint CM that deals with configuration.

Especially, you can use a property placeholder to easily load a configuration and inject some properties in your bean.

It's what we do in the `karaf-config-example-blueprint` feature:

```
karaf@root()> feature:install karaf-config-example-blueprint
```

You will see:

```
hello = world
```

Then, if you change `etc/org.apache.karaf.example.config.cfg` file to set `hello` property value to `other`, you will see:

```
hello = other
```

### SCR

SCR natively supports configuration.

It's what `karaf-config-example-scr` feature is using:

```
karaf@root()> karaf-config-example-scr
```

At installation time, we can see the configuration display:

```
service.pid = org.apache.karaf.example.config
hello = world
org.apache.karaf.features.configKey = org.apache.karaf.example.config
component.name = my-component
felix.fileinstall.filename = file:/home/jbonofre/Workspace/karaf/assemblies/apache-karaf/target/apache-karaf-4.2.3-SNAPSHOT/etc/org.apache.karaf.example.config.cfg
component.id = 1
foo = bar
```

Then, we can add a new property in `etc/org.apache.karaf.example.config.cfg` configuration file, it's displayed by the SCR component:

```
service.pid = org.apache.karaf.example.config
hello = world
org.apache.karaf.features.configKey = org.apache.karaf.example.config
component.name = my-component
felix.fileinstall.filename = file:/home/jbonofre/Workspace/karaf/assemblies/apache-karaf/target/apache-karaf-4.2.3-SNAPSHOT/etc/org.apache.karaf.example.config.cfg
component.id = 1
foo = bar
test = other
```