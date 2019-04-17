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

This example provides two kind of distributions:

* static distribution is very similar to Spring Boot approach, but supporting all Karaf powerful features.  The Karaf static (almost immutable) is a resolution performed at build time, and
we start all features (bundles set) at startup without the resolver. It's super fast, light and straight forward.
* dynamic distribution is applications container approach. It's the same kind of distribution as the Apache Karaf "vanilla" one: Karaf starts with a set of "boot" features. Then, you can deploy
new applications (at runtime) on the runtime instance.

## Artifacts

The project example is composed of three modules:

* `karaf-docker-example-app` is a regular Karaf application module. For this example, we implement a Servlet. We use the Karaf Maven plugin to automatically create a features XML that we will use for the distribution.
* `karaf-docker-example-static-dist` doesn't contain any code, just a `pom.xml`. This module creates a Karaf static distribution (almost immutable) ready to run, provide a turnkey Dockerfile and optionally create the corresponding Docker image.
* `karaf-docker-example-dynamic-dist` doesn't contain any code, just a `pom.xml`. This modules creates a Karaf dynamic distribution (applications container) ready to run, provide a turnkey Dockerfile and optionally create the corresponding Docker image.

### App

The `karaf-docker-example-app` is a regular Karaf application (packaged as a bundle). It uses SCR annotation to register
a `Servlet` service that the `http-whiteboard` feature will deploy.

It's possible to create a bunch of `app` modules like this one and include in several runtime.

To include the apps in the runtime (dist), we need a features XML file.

In the example, we simply use the Karaf Maven plugin `features-generate-descriptor` goal to automatically create the
features XML.

### Runtimes (distributions)

The `dist` modules create Karaf runtimes embedding the app.

#### Dynamic distribution

The `dynamic` distribution is the approach used by Apache Karaf vanilla distribution: it's a applications container approach.

You start the Karaf instance with "boot" applications, and then you can (at runtime) deploy new applications in the running instance. You can also update
the configuration, undeploy applications, etc at runtime.

We use the `karaf-maven-plugin` to create the distribution automatically:

1. the `assembly` goal creates the Karaf runtime filesystem, using the dependencies from the `pom.xml`
2. the `archive` goal creates an archive (tar.gz, zip) containing the Karaf runtime filesystem
3. the `dockerfile` goal creates a Dockerfile ready to create a Karaf Docker image
4. optionally, the `docker` goal can take the Dockerfile and directly use `docker` to create the Karaf Docker image.

#### Static distribution

On the other hand `static` distribution is a completely different approach where the resolution is made at build time.

We use the `karaf-maven-plugin` to create the distribution automatically:

1. the `assembly` goal creates the Karaf runtime filesystem, using the dependencies from the `pom.xml`
2. the `archive` goal creates an archive (tar.gz, zip) containing the Karaf runtime filesystem
3. the `dockerfile` goal creates a Dockerfile ready to create a Karaf Docker image
4. optionally, the `docker` goal can take the Dockerfile and directly use `docker` to create the Karaf Docker image.

The difference between dynamic and static distributions is just the configuration of the project and the `karaf-maven-plugin`.

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

### Static

Locally you can use `karaf-docker-example-static-dist/target/assembly`, with the `karaf run`:

```
$ karaf-docker-example-static-dist/target/assembly/bin/karaf run
```

You can also use the zip or tar.gz archive:

```
$ cd karaf-docker-example-static-dist/target
$ tar zxvf karaf-docker-example-static-dist-*.tar.gz
$ cd karaf-docker-example-static-dist*/bin
$ ./karaf run
```

You can build the docker image using the generated `Dockerfile`:

```
$ cd karaf-docker-example-static-dist/target
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
$ cd karaf-docker-example-static-dist
$ mvn clean install -Pdocker
```

```
$ docker images
REPOSITORY                                 TAG                 IMAGE ID            CREATED             SIZE
karaf-docker-example-static-dist           latest              226223b45024        6 seconds ago       463MB
```

You can use the docker image very easily (wherever you want):

```
$ docker run --name mykaraf -p 8181:8181 mykaraf
```

You can start the docker container in daemon mode:

```
$ docker run --name mykaraf -p 8181:8181 -d mykaraf
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

Now, in a web brower, you can test the Servlet using [http://localhost:8181/servlet-example].

[NOTE]
===
Update the host if you run the container somewhere else, like on a cloud.
===

You can push this docker image wherever you want, on a cloud provider for instance.

We can see the logs updated:

```
$ docker logs mykaraf
...
11:23:39.064 INFO  [qtp810811468-37] Client 172.17.0.1 request received on http://localhost:8181/servlet-example
```

### Dynamic

Locally you can use `karaf-docker-example-dynamic-dist/target/assembly`, with the `karaf`:

```
$ karaf-docker-example-dynamic-dist/target/assembly/bin/karaf
```

You can also use the zip or tar.gz archive:

```
$ cd karaf-docker-example-dynamic-dist/target
$ tar zxvf karaf-docker-example-dynamic-dist-*.tar.gz
$ cd karaf-docker-example-dynamic-dist*/bin
$ ./karaf
```

You can build the docker image using the generated `Dockerfile`:

```
$ cd karaf-docker-example-dynamic-dist/target
$ docker build -t mykaraf .
Sending build context to Docker daemon  102.5MB
Step 1/7 : FROM openjdk:8-jre
 ---> 19c48cc84cc6
Step 2/7 : ENV KARAF_INSTALL_PATH /opt
 ---> Running in 8a9a42db2395
Removing intermediate container 8a9a42db2395
 ---> 45f002fb0def
Step 3/7 : ENV KARAF_HOME $KARAF_INSTALL_PATH/apache-karaf
 ---> Running in c25e2a10d78e
Removing intermediate container c25e2a10d78e
 ---> ce1f0d7bdb0a
Step 4/7 : ENV PATH $PATH:$KARAF_HOME/bin
 ---> Running in 0fd126ff3a5f
Removing intermediate container 0fd126ff3a5f
 ---> 3ac6aaa5b072
Step 5/7 : COPY assembly $KARAF_HOME
 ---> 9a6a003d9845
Step 6/7 : EXPOSE 8101 1099 44444 8181
 ---> Running in 8e24a41487fb
Removing intermediate container 8e24a41487fb
 ---> 5643c771527c
Step 7/7 : CMD ["karaf"]
 ---> Running in 0e35ac21467d
Removing intermediate container 0e35ac21467d
 ---> d0cbfa94125c
Successfully built d0cbfa94125c
Successfully tagged mykaraf:latest

```

If you used the `docker` profile (`-Pdocker`) during the Maven build, you have the `karaf` docker image:

```
$ cd karaf-docker-example-dynamic-dist
$ mvn clean install -Pdocker
```

```
$ docker images
REPOSITORY                                 TAG                 IMAGE ID            CREATED              SIZE
karaf-docker-example-dynamic-dist          latest              38628803d8b2        3 seconds ago        479MB
```

You can use the docker image very easily (wherever you want):

```
$ docker run -i -t --name mykaraf -p 8181:8181 mykaraf
karaf: Ignoring predefined value for KARAF_HOME
        __ __                  ____      
       / //_/____ __________ _/ __/      
      / ,<  / __ `/ ___/ __ `/ /_        
     / /| |/ /_/ / /  / /_/ / __/        
    /_/ |_|\__,_/_/   \__,_/_/         

  Apache Karaf (4.3.0-SNAPSHOT)

Hit '<tab>' for a list of available commands
and '[cmd] --help' for help on a specific command.
Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown Karaf.

karaf@root()>  
```

We started here with the shell console. You can directly type Karaf shell commands here and interact the running
Karaf instance. 

[NOTE]
==
You can also override the startup command to use `karaf server`.
==

Now, in a web brower, you can test the Servlet using [http://localhost:8181/servlet-example].

we can see the request performed on our running Karaf instance:

```
karaf@root()> log:display
08:43:36.497 INFO [qtp1056947824-99] Client 172.17.0.1 request received on http://localhost:8181/servlet-example
```