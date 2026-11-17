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
# Apache Karaf Maven example

## Abstract

This example provides several Maven module showing usage of Karaf Maven plugin.

Each module uses a specific goal of the Karaf Maven plugin, covering different use cases.

## Maven Modules

* **karaf-maven-example-run** uses the `karaf-maven-plugin:run` goal to download and start a Karaf instance.
* **karaf-maven-example-run-bundle** uses the `karaf-maven-plugin:run` goal to download, start a Karaf instance and deploy the project bundle in this running instance.
* **karaf-maven-example-deploy** uses the `karaf-maven-plugin:deploy` goal to deploy the module artifact into a Karaf instance (remote for example).
* **karaf-maven-example-client** uses the `karaf-maven-plugin:client` goal to execute a shell command on a running Karaf instance.
* **karaf-maven-example-kar** packages a features repository as a kar file, ready to be deployed.
* **karaf-maven-example-kar-bundle** builds a bundle from the project sources and packages it with its features repository in a kar file.
* **karaf-maven-example-assembly** uses the `karaf-maven-plugin` to create a Karaf distribution.

## Goals Execution

### Run

The `karaf-maven-example-run` example downloads and starts a Apache Karaf container via Maven. By default, we just start and stop the instance (`<keepRunning>false</keepRunning>`),
but you can leave the instance running using `<keepRunning>true</keepRunning>`.

Simply do:

```
mvn clean install
```

### Run & Deploy

The `karaf-maven-example-run-bundle` example is similar to the `karaf-maven-example-run` adding the deployment on the current project bundle in the running instance.

The Karaf plugin is able to use the project artifact (jar, bundle, feature, whatever) when using `<deployProjectArtifact>true</deployProjectArtifact>`.
It will automatically try to deploy the module artifact into the running Karaf instance.

Simply do:

```
mvn clean install
```

### Deploy

The `karaf-maven-example-deploy` example expects a Apache Karaf instance running locally on your machine. Then, it will automatically deploy the project artifact
to this running instance (when `<useProjectArtifact/>` configuration is `true`, which is the default).

By default, it uses SSH, but you can use JMX instead (using `<useSsh>false</useSsh>` configuration).

It's also possible to deploy additional artifacts using `<artifactLocations/>` configuration.

Simply do:

```
mvn clean install
```

### Client

The `karaf-maven-example-client` example allows you to execute some shell commands on a running Karaf instance (using SSH) directly from Maven.

It allows you to install features, change configuration or whatever, and get the output within Maven.

It's possible to execute commands (specified with `<commands/>` configuration) and/or scripts (specified with `<scripts/>` configuration).

Simply do:

```
mvn clean install
```

### KAR

The `karaf-maven-example-kar` example shows how to create a KAR file containing project artifact.

Simply do:

```
mvn clean install
```

### KAR with the project bundle

The `kar` packaging doesn't compile Java sources. To package your own code in a KAR, the `karaf-maven-example-kar-bundle`
example uses the `bundle` packaging and adds two goals of the Karaf Maven plugin:

* `karaf:features-generate-descriptor` with `<includeProjectArtifact>true</includeProjectArtifact>` generates a features
  repository with a feature containing the project bundle. `<ignoreScopeProvided>true</ignoreScopeProvided>` keeps the
  `provided` dependencies (here `osgi.core`) out of the feature.
* `karaf:kar` packages the features repository and the bundle in a KAR file. Its `resourcesDir` is set to `src/main/kar`:
  the default (`target/classes`) would add the bundle classes at the root of the KAR. The `resources` folder of
  `src/main/kar` is copied in the Karaf base directory when the KAR is installed, here a configuration file in `etc`.

The KAR is attached next to the bundle, with the same coordinates and the `kar` extension. The features repository and
the bundle are in the same directory of the KAR:

```
repository/org/apache/karaf/examples/karaf-maven-example-kar-bundle/4.4.12-SNAPSHOT/karaf-maven-example-kar-bundle-4.4.12-SNAPSHOT-features.xml
repository/org/apache/karaf/examples/karaf-maven-example-kar-bundle/4.4.12-SNAPSHOT/karaf-maven-example-kar-bundle-4.4.12-SNAPSHOT.jar
resources/etc/org.apache.karaf.examples.maven.kar.cfg
```

You can also start from your own features XML in `src/main/feature/feature.xml` (Maven properties are filtered), the
plugin adds the project bundle to the feature named as the project `artifactId`.

Simply do:

```
mvn clean install
```

Then copy `target/karaf-maven-example-kar-bundle-4.4.12-SNAPSHOT.kar` in the `deploy` folder of a running Karaf instance
(or use `kar:install`). The `karaf-maven-example-kar-bundle` feature is installed and the bundle prints
`Hello from the KAR bundle !` on the console. The configuration is also loaded:

```
karaf@root()> config:list "(service.pid=org.apache.karaf.examples.maven.kar)"
```

### Assembly

The `karaf-maven-example-assembly` example shows how to create your own Karaf distribution easily.

Simply do:

```
mvn clean install
```