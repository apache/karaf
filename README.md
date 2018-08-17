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

[Apache Karaf](http://karaf.apache.org) is a modern and polymorphic applications container.
It's a lightweight, powered, and enterprise ready container powered by OSGi.
[Apache Karaf](http://karaf.apache.org) is a "product project", providing a complete and turnkey runtime. The runtime is
"multi-facets", meaning that you can deploy different kind of applications: OSGi or non OSGi, webapplication, services based, ...

## Overview

* Hot deployment: Karaf supports hot deployment of OSGi bundles by monitoring
    jar files inside the [home]/deploy directory. Each time a jar is copied in this folder,
    it will be installed inside the runtime. You can then update or delete it and changes will
    be handled automatically. In addition, the Kernel also supports exploded bundles and custom
    deployers (a spring one is included by default).
* Dynamic configuration: Services are usually configured through the ConfigurationAdmin OSGi
    service. Such configuration can be defined in Karaf using property files inside
    the [home]/etc directory. These configurations are monitored and changes on the properties
    files will be propagated to the services.
* Logging System: using a centralized logging back end supported by Log4J, Karaf
    supports a number of different APIs (JDK 1.4, JCL, SLF4J, Avalon, Tomcat, OSGi)
* Provisioning: Provisioning of libraries or applications can be done through a number of
    different ways, by which they will be downloaded locally, installed and started.
* Native OS integration: Karaf can be integrated into your own Operating System as
    a service so that the lifecycle will be bound to your Operating System.
* Extensible Shell console: Karaf features a nice text console where you can manage the
    services, install new applications or libraries and manage their state. This shell is easily
    extensible by deploying new commands dynamically along with new features or applications.
* Remote access: use any SSH client to connect to the kernel and issue commands in the console
* Security framework based on JAAS
* Managing instances: Karaf provides simple commands for managing instances of Karaf.
    You can easily create, delete, start and stop instances of Karaf through the console.

## Getting Started

For an Apache Karaf source distribution, please read [BUILDING.md](https://github.com/apache/karaf/BUILDING.md] for instructions on building Apache Karaf.

For an Apache Karaf binary distribution, please read [RELEASE-NOTES.md](https://github.com/apache/karaf/RELEASE-NOTES.md] for installation instructions and list of supported
and unsupported features.

The PDF manual is the right place to find any information about Karaf.

## Contact Us

To get involved in Apache Karaf:

* [Subscribe](mailto:user-subscribe@karaf.apache.org) or [mail](mailto:user@karaf.apache.org) the [user@karaf.apache.org](http://mail-archives.apache.org/mod_mbox/karaf-user/) list.
* [Subscribe](mailto:dev-subscribe@karaf.apache.org) or [mail](mailto:dev@karaf.apache.org) the [dev@karaf.apache.org](http://mail-archives.apache.org/mod_mbox/karaf-dev/) list.
* Report issues on [JIRA](https://issues.apache.org/jira/browse/KARAF).

We also have a [contributor's help](http://karaf.apache.org/community.html#contribute).

## More Information

* [Apache Karaf](http://karaf.apache.org)
* [Apache Karaf News](http://karaf.apache.org/news.html)
* [Apache Karaf Download](http://karaf.apache.org/download.html)
* [Apache Karaf Documentation](http://karaf.apache.org/documentation.html)
* [Apache Karaf Community](http://karaf.apache.org/community.html)
* [Apache Software Foundation](https://www.apache.org)

Many thanks for using Apache Karaf.

**The Apache Karaf Team**
