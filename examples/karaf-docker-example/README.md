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

# Apache Karaf Docker example

## Abstract

This example is a complete example how to easily create a Karaf powered application and package as a docker image, ready
to run anyway.

It's very similar to Spring Boot approach, but supporting all Karaf powerful features.

In this example, we use a Karaf static (almost immutable) approach: the resolution is performed at build time, and
we start all features (bundles set) at startup without the resolver.

It's super fast, super light and straight forward !

## Artifacts

The project example is composed of two modules:

* `karaf-docker-example-app` is a regular Karaf application module. For this example, we implement a Servlet. We use the Karaf Maven plugin to automatically create a features XML that we will use for the distribution.
* `karaf-docker-example-dist` doesn't contain any code, just a `pom.xml`. This module creates a Karaf static distribution ready to run, provide a turnkey Dockerfile and optionally create the corresponding Docker file.

### App

The `karaf-docker-example-app` is a regular Karaf application (packaged as a bundle). It uses SCR annotation to register
a `Servlet` service that the `http-whiteboard` feature will deploy.

It's possible to create a bunch of `app` modules like this one and include in several runtime.

To include the apps in the runtime (dist), we need a features XML file.

In the example, we simply use the Karaf Maven plugin `features-generate-descriptor` goal to automatically create the
features XML.

### Runtime (dist)

The `dist` module create the Karaf runtime embedding the app.

We can use here the "dynamic" approach (standard Karaf distribution), but for this example, we are using the "static" approach.

It's a completely different approach where the resolution is made at build time.

We use the `karaf-maven-plugin` to create the distribution automatically:

1. the `assembly` goal creates the Karaf runtime filesystem, using the dependencies from the `pom.xml`
2. the `archive` goal creates an archive (tar.gz, zip) containing the Karaf runtime filesystem
3. the `dockerfile` goal creates a Dockerfile ready to create a Karaf Docker image
4. optionally, the `docker` goal can take the Dockerfile and directly use `docker` to create the Karaf Docker image.

## Build

The build uses Apache Maven. Simply use:

```
$ mvn clean install
```

You will find the runtime in `karaf-docker-example-dist/target` folder (tar.gz and zip), and also the `Dockerfile`.

You can eventually directly create the Docker image using the `docker` profile (assuming `docker` is installed on your machine):

```
$ mvn clean install -Pdocker
```

## Usage

Locally you can use `karaf-docker-example-dist/target/assembly`, with the `karaf run`:

```
$ karaf-docker-example-dist/target/assembly/bin/karaf run
```

You can also use the zip or tar.gz archive:

```
$ cd karaf-docker-example-dist/target
$ tar zxvf karaf-docker-example-dist-*.tar.gz
$ cd karaf-docker-example-dist*/bin
$ ./karaf run
```

You can build the docker image using the generated `Dockerfile`:

```
$ cd karaf-docker-example-dist/target
$ docker build -t mykaraf .
Sending build context to Docker daemon  78.08MB
Step 1/7 : FROM openjdk:8-jre
 ---> d60154a7d9b2
Step 2/7 : ENV KARAF_INSTALL_PATH /opt
 ---> Using cache
 ---> ff77d7ccbfe3
Step 3/7 : ENV KARAF_HOME $KARAF_INSTALL_PATH/apache-karaf
 ---> Using cache
 ---> 0ebb21305f76
Step 4/7 : ENV PATH $PATH:$KARAF_HOME/bin
 ---> Using cache
 ---> 31bbe4c15205
Step 5/7 : COPY assembly $KARAF_HOME
 ---> Using cache
 ---> a9a88b63244e
Step 6/7 : EXPOSE 8101 1099 44444 8181
 ---> Using cache
 ---> 274550b95b48
Step 7/7 : CMD ["karaf", "run"]
 ---> Using cache
 ---> 7dbc9b63c058
Successfully built 7dbc9b63c058
Successfully tagged mykaraf:latest
```

If you used the `docker` profile (`-Pdocker`) during the Maven build, you have the `karaf` docker image:

```
$ cd karaf-docker-example-dist
$ mvn clean install -Pdocker
```

```
$ docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
karaf               latest              474fb876a819        3 seconds ago       463MB
```

You can use the docker image very easily (wherever you want):

```
$ docker run --name mykaraf -p 8181:8181 karaf
```

You can start the docker container in daemon mode:

```
$ docker run --name mykaraf -p 8181:8181 -d karaf
```

We can see our docker container running:

```
$ docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS                                     NAMES
d5f14a025286        karaf               "karaf run"         4 minutes ago       Up 4 minutes        1099/tcp, 8101/tcp, 8181/tcp, 44444/tcp   mykaraf
```

Then you can see the logs using `docker logs`:

```
$ docker logs mykaraf
karaf: Ignoring predefined value for KARAF_HOME
Mar 21, 2019 11:09:54 AM org.apache.karaf.main.Main launch
INFO: Installing and starting initial bundles
Mar 21, 2019 11:09:54 AM org.apache.karaf.main.Main launch
INFO: All initial bundles installed and set to start
Mar 21, 2019 11:09:54 AM org.apache.karaf.main.Main$KarafLockCallback lockAcquired
INFO: Lock acquired. Setting startlevel to 100
11:09:54.871 INFO  [FelixStartLevel] Logging initialized @811ms to org.eclipse.jetty.util.log.Slf4jLog
11:09:54.882 INFO  [FelixStartLevel] EventAdmin support is not available, no servlet events will be posted!
11:09:54.883 INFO  [FelixStartLevel] LogService support enabled, log events will be created.
11:09:54.885 INFO  [FelixStartLevel] Pax Web started
11:09:55.110 INFO  [paxweb-config-1-thread-1] No ALPN class available
11:09:55.111 INFO  [paxweb-config-1-thread-1] HTTP/2 not available, creating standard ServerConnector for Http
11:09:55.127 INFO  [paxweb-config-1-thread-1] Pax Web available at [0.0.0.0]:[8181]
11:09:55.131 INFO  [paxweb-config-1-thread-1] Binding bundle: [org.ops4j.pax.web.pax-web-extender-whiteboard [48]] to http service
11:09:55.143 INFO  [paxweb-config-1-thread-1] Binding bundle: [org.apache.karaf.examples.karaf-docker-example-app [15]] to http service
11:09:55.153 INFO  [paxweb-config-1-thread-1] will add org.eclipse.jetty.websocket.server.NativeWebSocketServletContainerInitializer to ServletContainerInitializers
11:09:55.154 INFO  [paxweb-config-1-thread-1] added ServletContainerInitializer: org.eclipse.jetty.websocket.server.NativeWebSocketServletContainerInitializer
11:09:55.154 INFO  [paxweb-config-1-thread-1] will add org.apache.jasper.servlet.JasperInitializer to ServletContainerInitializers
11:09:55.155 INFO  [paxweb-config-1-thread-1] Skipt org.apache.jasper.servlet.JasperInitializer, because specialized handler will be present
11:09:55.155 INFO  [paxweb-config-1-thread-1] will add org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer to ServletContainerInitializers
11:09:55.207 INFO  [paxweb-config-1-thread-1] added ServletContainerInitializer: org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer
11:09:55.241 INFO  [paxweb-config-1-thread-1] registering context DefaultHttpContext [bundle=org.apache.karaf.examples.karaf-docker-example-app [15], contextID=default], with context-name:
11:09:55.251 INFO  [paxweb-config-1-thread-1] registering JasperInitializer
11:09:55.282 INFO  [paxweb-config-1-thread-1] No DecoratedObjectFactory provided, using new org.eclipse.jetty.util.DecoratedObjectFactory[decorators=1]
11:09:55.354 INFO  [paxweb-config-1-thread-1] DefaultSessionIdManager workerName=node0
11:09:55.354 INFO  [paxweb-config-1-thread-1] No SessionScavenger set, using defaults
11:09:55.355 INFO  [paxweb-config-1-thread-1] node0 Scavenging every 600000ms
11:09:55.364 INFO  [paxweb-config-1-thread-1] Started HttpServiceContext{httpContext=DefaultHttpContext [bundle=org.apache.karaf.examples.karaf-docker-example-app [15], contextID=default]}
11:09:55.369 INFO  [paxweb-config-1-thread-1] jetty-9.4.12.v20180830; built: 2018-08-30T13:59:14.071Z; git: 27208684755d94a92186989f695db2d7b21ebc51; jvm 1.8.0_181-8u181-b13-2~deb9u1-b13
11:09:55.403 INFO  [paxweb-config-1-thread-1] Started default@437b18f2{HTTP/1.1,[http/1.1]}{0.0.0.0:8181}
11:09:55.403 INFO  [paxweb-config-1-thread-1] Started @1347ms
11:09:55.405 INFO  [paxweb-config-1-thread-1] Binding bundle: [org.apache.karaf.http.core [16]] to http service
```

Now, in a web brower, you can test the Servlet using http://localhost:8181/servlet-example

NB: update the host if you run the container somewhere else, like on a cloud.

You can push this docker image wherever you want, on a cloud provider for instance.

We can see the logs updated:

```
$ docker logs mykaraf
...
11:23:39.064 INFO  [qtp810811468-37] Client 172.17.0.1 request received on http://localhost:8181/servlet-example
```