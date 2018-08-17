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
# Apache Karaf docker

## Installation

Install the most recent stable version of docker
https://docs.docker.com/installation/

Install the most recent stable version of docker-compose
https://docs.docker.com/compose/install/

## Build

Images are based on the official Java Alpine (OpenJDK 8) image. If you want to
build the Karaf image run:

```
sh build.sh
```

or

```
docker build -t karaf .
```

If you want to build the container for a specific version of Karaf
you can configure it with the `KARAF_VERSION` arg:

```
docker build --build-arg KARAF_VERSION=4.2.0 -t "karaf:4.2.0" karaf
```

## Run

* Run Karaf with interactive mode

```
docker-compose run karaf
```

* Run Karaf as a daemon (without interaction)

```
    docker-compose up
```

* Kill Karaf

```
docker-compose kill
```

### Ports

* The Karaf SSH server is on `8101`
* The Karaf WebContainer is on `8888`
* The Karaf JMX MBean server is on `1099` (default, not exposed to host) and `44444` (default, not exposed to host)

Edit the `docker-compose.yml` file to edit port settings.