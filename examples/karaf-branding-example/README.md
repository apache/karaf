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
# Apache Karaf Branding Example

## Abstract

You can change the look'n feel (branding) of the Karaf shell console to provide your own Karaf distribution.
It's also possible to brand the Karaf SSH shell.

To do so, you have two solutions:

* Provide a `branding.properties` file in the Karaf `etc` folder.
* Provide a bundle providing the branding

## Using `etc/branding.properties` file

It's probably the easiest way to customize the Karaf shell console.

The `branding.properties` basically contains the "welcome message" or "message of the day" (displayed when Karaf starts).


_NB: It's also possible to configure the shell prompt in the `prompt` property in `etc/shell.init.script`._

For instance, the following `branding.properties`:

```
welcome = \
\n\
\u001B[1m  My Lovely Apache Karaf Branding\u001B[0m (x.x.x)\n\
\n\
Hit '\u001B[1m<tab>\u001B[0m' for a list of available commands\n\
   and '\u001B[1m[cmd] --help\u001B[0m' for help on a specific command.\n
```

Note here the using of the ASCII code (`\u001B[1m`) allowing you to use bold, italic, color, ... text in the shell console.

The only drawback of this approach is the `branding.properties` is "visible" and can be changed by an end-user.

The second solution using a bundle allows you to "hide" the `branding.properties` file.

## Using branding bundle

This example also provides a branding bundle wrapping the `branding.properties` file.

A branding bundle has to wrap a `org.apache.karaf.branding` package containing the `branding.properties` file.

### Artifacts

The `karaf-branding-example` bundle is the one providing our custom branding properties file.

### Build

To build the `karaf-branding-example` bundle, you just have to do:

```
mvn clean install
```

### Deployment

The `karaf-branding-example` bundle has to be part of the startup bundles. You have two ways to register your custom
branding bundle:

1. You can define the `karaf-branding-example` bundle URL (`mvn:org.apache.karaf.examples/karaf-branding-example/LATEST`) in the `etc/startup.properties` file.
2. You can create your own custom profile (see profile example for detail), defining `karaf-branding-example` bundle URL (`bundle.mvn\:org.apache.karaf.examples/karaf-branding-example/LATEST = mvn:org.apache.karaf.examples/karaf-branding-starter/LATEST`) in the `etc/profile.cfg` file. 