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

Karaf Bundle EXAMPLE
====================

Purpose
-------
Demonstrate how to build an OSGi Bundle.


Prerequisites for Running this Example
--------------------------------------
You must have the following installed on your machine:

   - JDK 1.6 or higher.

   - Apache Maven 3.0.3 or higher.


Building and Deploying
----------------------
This example will produce an OSGi bundle (a special type
of Java Archive (jar).

To build the demo bundle invoke the following command:

  mvn install

To deploy the newly created bundle you may either directly
copy the generated my-bundle-1.0.0-SNAPSHOT.jar to the deploy
directory, or issue the following command on the Karaf
console:

  karaf@root> bundle:install -s mvn:org.apache.karaf.demos.deployer.bundle/my-bundle/1.0.0-SNAPSHOT
  Starting the Apache Karaf demo bundle
  Bundle ID: 73 

Upon installation the bundle was immediately started, printing
the message in its activator's start method. To further verify
its successful deployment invoke the following:

  karaf@root> list
  START LEVEL 100 , List Threshold: 50
   ID   State         Level  Name
  [  73] [    Active] [   80] Apache Karaf :: Demos :: Deployer :: Bundle (1.0.0.SNAPSHOT)

The bundle may be started or stopped via the start and stop
commands. A message will be printed to the console each time
this occurs. 

Note: Using Karaf archetype to generate a bundle project:
---------------------------------------------------------

The karaf-bundle-archetype creates a Maven skeleton project
to build an OSGi bundle with Activator (a special callback
class for bundle start/stop).

On the command line issue the following command to create
a bundle project:

  mvn archetype:generate \
      -DarchetypeGroupId=org.apache.karaf.archetypes \ 
      -DarchetypeArtifactId=karaf-bundle-archetype \ 
      -DarchetypeVersion=${version} \ 
      -DgroupId=org.myorg \
      -DartifactId=my-bundle \ 
      -Dversion=1.0-SNAPSHOT \ 
      -Dpackage=org.myorg.package

For more information on using the karaf-bundle-archetype
please visit: http://karaf.apache.org/manual/latest-3.0.x/developers-guide/archetypes.html
