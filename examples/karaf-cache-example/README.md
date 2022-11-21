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
# Apache Karaf Cache example

## Abstract

This example shows how to use a simple cache facade provided by Karaf.

## Build
The build uses Apache Maven. Simply use:

```
mvn clean install
```

## Deployment
On a running Karaf instance, add a feature repository and then the feature:
```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-cache-example-feature/LATEST/xml
karaf@root()> feature:install karaf-cache-example
```

## Usage
Check the source code of karaf-cache-example-client to see how to programmatically use Karaf's *CacheService* by creating a cache with Ehcache configuration builders or by providing XML configuration file. When using XML config files, a *ClassLoader* needs to be passed so Ehcache can load keys and values other than standard Java types like String and Long, otherwise it won't work in OSGi environment. It can also be passed in the *Configuration* object.

*CacheService* can be obtained via standard OSGi practices such as *ServiceTracker*, Blueprint or Declarative Services.


You can try out the *cache* commands:
```
karaf@root()> cache:list
ExampleCache
BookCache

karaf@root()> cache:get BookCache 1
Book{title='Apache Karaf Cookbook', numOfPages=789}
karaf@root()> cache:invalidate BookCache
BookCache invalidated
karaf@root()> cache:get BookCache 1
Error executing command: Cache BookCache not found!

```
To create a new cache via a command, put an Ehcache XML configuration file in your Karaf's etc directory, for example:
```
<ehcache:config xmlns:ehcache="http://www.ehcache.org/v3">
    <ehcache:cache alias="TestCache">
        <ehcache:key-type>java.lang.Long</ehcache:key-type>
        <ehcache:value-type>java.lang.String</ehcache:value-type>

        <ehcache:expiry>
            <ehcache:tti unit="minutes">2</ehcache:tti>
        </ehcache:expiry>

        <ehcache:heap unit="entries">200</ehcache:heap>
    </ehcache:cache>
</ehcache:config>

```
In this case the file name is `config.xml`.
```
karaf@root()> cache:create config.xml
karaf@root()> cache:put TestCache 1 Hello
karaf@root()> cache:put TestCache 2 World
karaf@root()> cache:get TestCache 1
Hello
karaf@root()> cache:get TestCache 2
World
```



