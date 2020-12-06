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
# Apache Karaf CDI Example

## Abstract

It's possible to use CDI in your applications, including sharing bean between bundles using the Karaf service registry.

In addition of supporting the regular CDI annotations (`@ApplicationScoped`, `@Inject`, etc), Karaf CDI also supports
couple of new OSGi CDI annotations: `@Service` and `@Reference`.

In this example, we are using Aries CDI and OpenWebBeans as CDI container.

## Artifacts

* `karaf-cdi-example-api` provides API (interface) shared between provider and consumer bundles. It allows a decoupling between interface and bean implementation.
* `karaf-cdi-example-provider` provides an application scope bean and using the `@Service` annotation to register the bean in the Karaf service registry (can be used in another bundle).
* `karaf-cdi-example-consumer` creates another bean and inject the provider bean from the registry (thanks to the `@Reference` annotation).
* `karaf-cdi-example-features` provides a Karaf features XML to easily install the example application.

##Build 

Simply use:

```
mvn clean install
```

##Feature and Deployment

On a running Karaf instance, you register the CDI example features repository with:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-cdi-example-features/LATEST/xml
```

Then you can install the `karaf-cdi-example-provider` feature:

```
karaf@root()> feature:install karaf-cdi-example-provider
```

Then, you can install the `karaf-cdi-example-consumer` feature:

```
karaf@root()> feature:install karaf-cdi-example-consumer
```

When you install the consumer feature, you will see:

```
Hello world
```
