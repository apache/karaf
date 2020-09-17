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

If you want to build multi-platform (OS/Arch) Docker images, then you must install
[`buildx`](https://docs.docker.com/buildx/working-with-buildx/).
On macOS, an easy way to install `buildx` is to install [Docker Desktop Edge](https://docs.docker.com/docker-for-mac/edge-release-notes/).

## Build

Images are based on the Docker official [AdoptOpenJDK 11 JRE Hotspot](https://hub.docker.com/_/adoptopenjdk?tab=tags&page=1&name=11-jre-hotspot) image. If you want to
build the Karaf image you have the following choices:

1. Create the docker image from a local distribution package
2. Create the docker image from an Apache Karaf archive, for example (apache-karaf-4.2.9.tar.gz)
3. Create the docker image from a specific version of Apache Karaf
4. Create the docker image from remote or local custom Apache Karaf distribution

If you run `build.sh` without arguments then you could see how to usage this command.

```bash
Usage:
  build.sh --from-local-dist [--archive <archive>] [--image-name <image>] [--build-multi-platform <comma-separated platforms>]
  build.sh --from-release --karaf-version <x.x.x> [--image-name <image>] [--build-multi-platform <comma-separated platforms>]
  build.sh --help

  If the --image-name flag is not used the built image name will be 'karaf'.
  Check the supported build platforms; you can verify with this command: docker buildx ls
  The supported platforms (OS/Arch) depend on the build's base image, in this case [adoptopenjdk:11-jre-hotspot](https://hub.docker.com/_/adoptopenjdk?tab=tags&page=1&name=11-jre-hotspot).
```

To create the docker image from local distribution) you can execute the command 
below. Remember that before you can successfully run this command, you must build 
the project (for example with the command `mvn clean install -DskipTests`). 
For more info you can read: 
[Building Apache Karaf](https://github.com/apache/karaf/blob/master/BUILDING.md#building-apache-karaf)

```bash
./build.sh --from-local-dist
```

For create the docker image from the local dist version but with the archive,
you can execute the below command. Remember that before you can successfully run 
this command.

```bash
./build.sh --from-local-dist --archive ~/home/amusarra/apache-karaf-4.2.9.tar.gz
```

You can also specify the image name with the `--image-name` flag, for example
(replacing the version, image name, and targets as appropriate):

```bash
./build.sh --from-local-dist --archive ~/Downloads/apache-karaf-4.2.9.tar.gz --image-name myrepo/mykaraf:x.x.x
```

If you want to build the docker image for a specific version of Karaf
you can run `build.sh` command in this way (replacing the version, image name, 
and targets as appropriate):

```bash
./build.sh --from-release --karaf-version 4.2.9 --image-name myrepo/mykaraf:x.x.x
```

If you want to build the container for a specific version of Karaf and
specific version of the platform, and push the image to the Docker Hub repository,
you can use this command (replacing the version, image name, and targets as appropriate):

```bash
./build.sh --from-release --karaf-version 4.2.9 --image-name myrepo/mykaraf:x.x.x \
 --build-multi-platform linux/arm64,linux/arm/v7,linux/amd64
```

Below is the output you should get from running the previous command.

```
Downloading apache-karaf-4.2.9.tar.gz from https://downloads.apache.org/karaf/4.2.9/
Checking if buildx installed...
Found buildx {github.com/docker/buildx v0.3.1-tp-docker 6db68d029599c6710a32aa7adcba8e5a344795a7} on your docker system
Starting build of the docker image for the platform linux/arm64,linux/arm/v7,linux/amd64
[+] Building 15.8s (16/16) FINISHED
 => [internal] load build definition from Dockerfile                                                                                                                         0.0s
 => => transferring dockerfile: 32B                                                                                                                                          0.0s
 => [internal] load .dockerignore                                                                                                                                            0.1s
 => => transferring context: 2B                                                                                                                                              0.0s
 => [linux/arm64 internal] load metadata for docker.io/library/openjdk:8u212-jre-alpine                                                                                      2.5s
 => [linux/arm/v7 internal] load metadata for docker.io/library/openjdk:8u212-jre-alpine                                                                                     2.6s
 => [linux/amd64 internal] load metadata for docker.io/library/openjdk:8u212-jre-alpine                                                                                      2.5s
 => [linux/amd64 1/3] FROM docker.io/library/openjdk:8u212-jre-alpine@sha256:f362b165b870ef129cbe730f29065ff37399c0aa8bcab3e44b51c302938c9193                                0.0s
 => => resolve docker.io/library/openjdk:8u212-jre-alpine@sha256:f362b165b870ef129cbe730f29065ff37399c0aa8bcab3e44b51c302938c9193                                            0.0s
 => [internal] load build context                                                                                                                                            1.7s
 => => transferring context: 22.69MB                                                                                                                                         1.7s
 => [linux/arm64 1/3] FROM docker.io/library/openjdk:8u212-jre-alpine@sha256:f362b165b870ef129cbe730f29065ff37399c0aa8bcab3e44b51c302938c9193                                0.0s
 => [linux/arm/v7 1/3] FROM docker.io/library/openjdk:8u212-jre-alpine@sha256:f362b165b870ef129cbe730f29065ff37399c0aa8bcab3e44b51c302938c9193                               0.0s
 => CACHED [linux/arm64 2/3] ADD _TMP_/apache-karaf-4.2.9.tar.gz /opt                                                                                                        0.0s
 => CACHED [linux/arm64 3/3] RUN set -x &&   ln -s /opt/apache-karaf* /opt/apache-karaf                                                                                      0.0s
 => CACHED [linux/amd64 2/3] ADD _TMP_/apache-karaf-4.2.9.tar.gz /opt                                                                                                        0.0s
 => CACHED [linux/amd64 3/3] RUN set -x &&   ln -s /opt/apache-karaf* /opt/apache-karaf                                                                                      0.0s
 => CACHED [linux/arm/v7 2/3] ADD _TMP_/apache-karaf-4.2.9.tar.gz /opt                                                                                                       0.0s
 => CACHED [linux/arm/v7 3/3] RUN set -x &&   ln -s /opt/apache-karaf* /opt/apache-karaf                                                                                     0.0s
 => exporting to image                                                                                                                                                      11.4s
 => => exporting layers                                                                                                                                                      0.0s
 => => exporting manifest sha256:205fe4347d83e0183ff479360733d6294a8d060127d5a87ae0e06e1f9b18f08e                                                                            0.0s
 => => exporting config sha256:94a9fb41574916225852575d3f1eda6f267d3e83dfc81e262e8a766b5b36e92f                                                                              0.0s
 => => exporting manifest sha256:acb94fccec1b975a62b7385cf227e01afb5875d74c24a5bef4546381fd2a483e                                                                            0.0s
 => => exporting config sha256:d2b0989d52cd13f19a02f7e88544f1e184bc592178608fd79c88d635f751707a                                                                              0.0s
 => => exporting manifest sha256:47c72f3cb18db75f63c21da1e475958580f3d9c935930e1e8b04ccc7ad0e8a37                                                                            0.0s
 => => exporting config sha256:13d3f0dc19bec1fec60767a4f5d19750f02401efa0aabc7f83fc318a96eaf660                                                                              0.0s
 => => exporting manifest list sha256:341588c548bfe56818c6435d3301fee6e2e6b4b06e5bb94b15102c0ca86a90e9                                                                       0.0s
 => => pushing layers                                                                                                                                                        3.7s
 => => pushing manifest for docker.io/amusarra/karaf:4.2.9
```

## Run

* Run Karaf with interactive mode

```
docker-compose run karaf karaf
```

or 

```
docker run --name karaf karaf karaf
```

* Run Karaf as a daemon (without interaction)

```
docker-compose up
```

or 

```
docker run --name karaf
```

* Kill Karaf

```
docker-compose kill
```

or

```
docker kill karaf
```

### Ports

* The Karaf SSH server is on `8101`
* The Karaf WebContainer is on `8888`
* The Karaf JMX MBean server is on `1099` (default, not exposed to host) and `44444` (default, not exposed to host)

Edit the `docker-compose.yml` file to edit port settings.
