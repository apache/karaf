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

You have three ways to deploy the example war.

1. Using the war deployer
2. Using the bundle:install shell command (with webbundle protocol)
3. Using the provided feature

### WAR deployer

The WAR deployer allows you to directly hotdeploy war files, by simpling dropping the war files in the Karaf `deploy` folder.

To enable the WAR deployer, you have to install the `war` feature first:

```
karaf@root()> feature:install war
```

Then, simply drop the war file in the `deploy` folder:

```
$ cp examples/karaf-war-example/karaf-war-example-webapp/target/karaf-war-example-webapp-*.war $KARAF_HOME/deploy
```

You can then see the webapp deployed using the `web:list` command:

```
karaf@root()> web:list
ID  │ State       │ Web-State   │ Level │ Web-ContextPath           │ Name
────┼─────────────┼─────────────┼───────┼───────────────────────────┼──────────────────────────
102 │ Active      │ Deployed    │ 80    │ /karaf-war-example-webapp │ karaf-war-example-webapp
```

You can point your browser to [http://localhost:8181/karaf-war-example-webapp].

### Shell command (webbundle)

You can also install the war directly from a location using the "classic" `bundle:install` command.
As a war is not a "regular" bundle, we need to use the `webbundle` protocol to specify the artifact is actually a war.

First, we install the `war` feature which provides the `webbundle` protocol:

```
karaf@root()> feature:install war
```

Then, we can directly install a war from any location, for instance:

```
karaf@root()> bundle:install -s webbundle:mvn:org.apache.karaf.examples/karaf-war-example-webapp/4.3.0-SNAPSHOT/war?Web-ContextPath=example
```

You can see the war deployed using `web:list` command:

```
karaf@root()> web:list
ID  │ State       │ Web-State   │ Level │ Web-ContextPath │ Name
────┼─────────────┼─────────────┼───────┼─────────────────┼──────────────────────────────────────────────────────────────────────────────
102 │ Active      │ Deployed    │ 80    │ /example        │ mvn_org.apache.karaf.examples_karaf-war-example-webapp_4.3.0-SNAPSHOT_war (0)
```

We specify the context path on the URL. You can then point your browser to [http://localhost:8181/example].

### Feature

You can use the `webbundle` protocol on URL directly in a features repository.

It's illustrated in the `karaf-war-example-features` features repository.

Then, you simply have to add this features repository and directly install the `karaf-war-example` feature containing the war. This feature automatically installs the `war` feature, all in a row.

First, you add the features repository:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-war-example-features/4.3.0-SNAPSHOT/xml
Adding feature url mvn:org.apache.karaf.examples/karaf-war-example-features/4.3.0-SNAPSHOT/xml
```

Then, you can install the `karaf-war-example`:

```
karaf@root()> feature:install karaf-war-example
```

You can see the war deployed using `web:list` command:

```
karaf@root()> web:list
ID  │ State       │ Web-State   │ Level │ Web-ContextPath │ Name
────┼─────────────┼─────────────┼───────┼─────────────────┼─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
102 │ Active      │ Deployed    │ 80    │ /example        │ file__home_jbonofre_.m2_repository_org_apache_karaf_examples_karaf-war-example-webapp_4.3.0-SNAPSHOT_karaf-war-example-webapp-4.3.0-SNAPSHOT.war (0)
```

You can then point your browser to [http://localhost:8181/example].