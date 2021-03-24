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

# Apache Karaf

[Apache Karaf](https://karaf.apache.org) is a modulith runtime, supporting several frameworks and programming model (REST/API, web, spring boot, ...).
It provides turnkey features that you can directly leverage without effort, packaged as mutable or immutable application.

## Overview

* **Hot deployment**: Karaf supports hot deployment of applications (in the deploy directory).
* **Dynamic configuration**: Karaf uses a central location (etc directory) for configuration 
    (in different format, properties, json) and can be plug on existing configuration backend.
* **Logging System**: using a centralized logging back end supported by Log4J, Karaf
    supports a number of different APIs (JDK 1.4, JCL, SLF4J, Avalon, Tomcat, ...)
* **Provisioning**: Provisioning of libraries or applications can be done through a number of
    different ways, by which they will be downloaded locally, installed and started. It interacts
    with the resolver to automatically install the required components.
* **Extensible Shell console**: Karaf features a nice text console where you can manage the
    services, install new applications or libraries and manage their state. This shell is easily
    extensible by deploying new commands dynamically along with new features or applications.
* **Remote access**: use any SSH client to connect to the kernel and issue commands in the console
* **Security & ACL** framework based on JAAS providing complete RBAC solution.
* **Managing instances**: Karaf provides simple commands for managing instances of Karaf.
    You can easily create, delete, start and stop instances of Karaf through the console.
* **Enterprise features**: Karaf provides a bunch of enterprise features that you can use in your applications (JDBC, JPA, JTA, JMS, ...).
* **HTTP Service**: Karaf provides a full features web container, allowing you to deploy your web applications.
* **REST & Services**: Karaf supports different service frameworks as Apache CXF allowing you to easily implements your services.
* **Karaf Extensions**: Karaf project is a complete ecosystem. The runtime can be extended by other Karaf subprojects such as Karaf Decanter, Karaf Cellar, Karaf Cave, ...
* **Third Party Extensions**: Karaf is a supported runtime for a lot of other projects as [Apache Camel](https://camel.apache.org), and much more.

## Getting Started

For an Apache Karaf source distribution, please read [BUILDING.md](https://github.com/apache/karaf/blob/main/BUILDING.md) for instructions on building Apache Karaf.

For an Apache Karaf binary distribution, please read [RELEASE-NOTES.md](https://github.com/apache/karaf/blob/main/RELEASE-NOTES.md) for installation instructions and list of supported
and unsupported features.

The PDF manual is the right place to find any information about Karaf.

The [examples](https://github.com/apache/karaf/tree/main/examples) provide a bunch of turnkey minimal applications that you can deploy in Apache Karaf and extend/template as you want.

[NOTE]
====
Windows users should use 7zip or other unzip tool to support files longer than 255 characters.
====

## Contact Us

To get involved in Apache Karaf:

* [Subscribe](mailto:user-subscribe@karaf.apache.org) or [mail](mailto:user@karaf.apache.org) the [user@karaf.apache.org](https://mail-archives.apache.org/mod_mbox/karaf-user/) list.
* [Subscribe](mailto:dev-subscribe@karaf.apache.org) or [mail](mailto:dev@karaf.apache.org) the [dev@karaf.apache.org](https://mail-archives.apache.org/mod_mbox/karaf-dev/) list.
* Report issues on [JIRA](https://issues.apache.org/jira/browse/KARAF).

We also have a [contributor's guide](https://karaf.apache.org/community.html#contribute).

## More Information

* [Apache Karaf](https://karaf.apache.org)
* [Apache Karaf News](https://karaf.apache.org/news.html)
* [Apache Karaf Download](https://karaf.apache.org/download.html)
* [Apache Karaf Documentation](https://karaf.apache.org/documentation.html)
* [Apache Karaf Community](https://karaf.apache.org/community.html)
* [Apache Software Foundation](https://www.apache.org)

Many thanks for using Apache Karaf.

**The Apache Karaf Team**
