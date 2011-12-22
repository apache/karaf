/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

Karaf Archive (kar)  EXAMPLE
==============================

Purpose
-------
Demonstrate how to create Karaf Archives.


Prerequisites for Running this Example
--------------------------------------
You must have the following installed on your machine:

   - JDK 1.6 or higher.

   - Apache Maven 2.2.1 or higher.


Building and Deploying
----------------------
This example will produce a kar, containing a bundle. 

To build the demo kar invoke the following command:

  mvn install

Karaf provides a KAR deployer:

  karaf@root> la | grep -i archive
  [  15] [Active     ] [Created     ] [   30] Apache Karaf :: Deployer :: Karaf Archive (.kar) (${pom.version})

It's a core deployer (you don't need to install additional features).

To deploy a kar, simply drop the kar into the deploy directory. 
The KAR Deployer will deploy all the kar content starting from
the features descriptor.

The KAR Deployer creates a repository dedicated to your kar 
(in the $/local-repo) and register the features descriptor.
You can now see your feature available for installation:

  karaf@root> feature:list | grep -i my-kar
  [installed] [${pom.version}             ] my-kar                        repo-0

Now you can use any commands available on features:

  karaf@root> feature:info my-kar
  Feature my-kar ${pom.version}
  Feature has no configuration
  Feature has no configuration files
  Feature has no dependencies.
  Feature contains followed bundles:
    mvn:commons-collections/commons-collections/3.2.1

For more information on Karaf Archives please visit:
http://karaf.apache.org/manual/latest-2.2.x/users-guide/kar.html
