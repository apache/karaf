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
# Apache Karaf JAAS security example

## Abstract

This example shows different usage of the Karaf Security service.

It uses the `karaf` security realm to secure actions, services, or web application URLs.

## Secure service & shell command

The `karaf-jaas-example-app` shows a very simple application that use Karaf `security` implicitly to authenticate and
authorize an user.

You can install the `karaf-jaas-example-app` using the corresponding feature:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-jaas-example-features/LATEST/xml
karaf@root()> feature:install karaf-jaas-example-app
```

The application provides the `example:jaas` shell command. This command takes an username and password at argument, which are passed to the `SecuredService`.
Then, the `SecuredService` authenticates and authorizes the username/password using the `karaf` realm.

If you use `karaf`/`karaf` (default user in Karaf), you will see:

```
karaf@root()> example:jaas karaf karaf
Authentication successful
```

If you use any invalid username/password, you will see:

```
karaf@root()> example:jaas foo bar
Error executing command: login failed
```

## Secure web bundle & servlet

You can install a simple web application bundle using the `karaf-jaas-example-wab` feature:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-jaas-example-features/LATEST/xml
karaf@root()> feature:install karaf-jaas-example-wab
```

The web application is registered on `/example` URL.

The web application context is restricted (using HTTP basic authentication) to user with `admin` role.

So, if you point your Internet browser to `http://localhost:8181/example`, you will have to enter username/password.

`karaf`/`karaf` will work as this user has the `admin` role (see in `etc/users.properties`).

If you enter `foo`/`bar` for instance, you won't be able to see the page.
 
## Secure war

You can install a simple war containing a `index.jsp` and a secure configuration in `WEB-INF/web.xml`.

To install the war, you can use the `karaf-jaas-example-war` feature:

```
karaf@root()> feature:repo-add mvn:org.apache.karaf.examples/karaf-jaas-example-features/LATEST/xml
karaf@root()> feature:install karaf-jaas-example-war
```

The WAR is deployed on `/example` and secured.

It means that if you use `http://localhost:8181/example` in a browser, you will have to enter an username and password.

It's again the `karaf` realm used. So if you enter `karaf`/`karaf`, you will be able to see the home page.

On the other hand, if you enter `foo`/`bar` for instance, you won't be able to access the home page.